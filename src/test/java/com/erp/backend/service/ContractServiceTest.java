package com.erp.backend.service;

import com.erp.backend.domain.Contract;
import com.erp.backend.domain.ContractStatus;
import com.erp.backend.domain.Customer;
import com.erp.backend.domain.Subscription;
import com.erp.backend.domain.SubscriptionStatus;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.ContractRepository;
import com.erp.backend.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: ContractService")
class ContractServiceTest {

    @Mock private ContractRepository contractRepository;
    @Mock private CustomerRepository customerRepository;

    @InjectMocks
    private ContractService contractService;

    private UUID contractId;
    private UUID customerId;
    private Customer customer;
    private Contract validContract;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        customer = new Customer("Max", "Mustermann", "max@test.de", "+49123");
        customer.setId(customerId);

        validContract = new Contract("Testvertrag", LocalDate.now(), customer);
        validContract.setId(contractId);
        validContract.setContractStatus(ContractStatus.DRAFT);
    }

    // ========== createContract ==========

    @Test
    @DisplayName("Vertrag wird erfolgreich erstellt")
    void createContract_success() {
        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(contractRepository.findByContractNumber(any())).thenReturn(Optional.empty());
        when(contractRepository.save(any(Contract.class))).thenReturn(validContract);

        Contract result = contractService.createContract(validContract);

        assertThat(result.getContractTitle()).isEqualTo("Testvertrag");
        verify(contractRepository, times(1)).save(any(Contract.class));
    }

    @Test
    @DisplayName("Erstellen ohne Titel wirft BusinessLogicException")
    void createContract_missingTitle_throwsBusinessLogicException() {
        Contract noTitle = new Contract(null, LocalDate.now(), customer);
        noTitle.setCustomer(customer);

        assertThatThrownBy(() -> contractService.createContract(noTitle))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("Vertragstitel");

        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Erstellen ohne Startdatum wirft BusinessLogicException")
    void createContract_missingStartDate_throwsBusinessLogicException() {
        Contract noDate = new Contract("Titel", null, customer);
        noDate.setCustomer(customer);

        assertThatThrownBy(() -> contractService.createContract(noDate))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("Startdatum");
    }

    @Test
    @DisplayName("Erstellen mit unbekanntem Kunden wirft ResourceNotFoundException")
    void createContract_customerNotFound_throwsResourceNotFoundException() {
        when(customerRepository.existsById(customerId)).thenReturn(false);

        assertThatThrownBy(() -> contractService.createContract(validContract))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(customerId.toString());
    }

    // ========== updateContract ==========

    @Test
    @DisplayName("Update ohne ID wirft BusinessLogicException")
    void updateContract_nullId_throwsBusinessLogicException() {
        validContract.setId(null);

        assertThatThrownBy(() -> contractService.updateContract(validContract))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    @DisplayName("Update mit unbekannter ID wirft ResourceNotFoundException")
    void updateContract_notFound_throwsResourceNotFoundException() {
        when(contractRepository.existsById(contractId)).thenReturn(false);

        assertThatThrownBy(() -> contractService.updateContract(validContract))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(contractId.toString());
    }

    // ========== deleteContract ==========

    @Test
    @DisplayName("Vertrag ohne aktive Abonnements kann gelöscht werden")
    void deleteContract_noActiveSubscriptions_success() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(validContract));

        contractService.deleteContract(contractId);

        verify(contractRepository, times(1)).delete(validContract);
    }

    @Test
    @DisplayName("Vertrag mit aktiven Abonnements kann nicht gelöscht werden")
    void deleteContract_withActiveSubscriptions_throwsBusinessLogicException() {
        Subscription activeSub = new Subscription();
        activeSub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        validContract.getSubscriptions().add(activeSub);

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(validContract));

        assertThatThrownBy(() -> contractService.deleteContract(contractId))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("aktive Abonnements");

        verify(contractRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Löschen eines nicht vorhandenen Vertrags wirft ResourceNotFoundException")
    void deleteContract_notFound_throwsResourceNotFoundException() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.deleteContract(contractId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(contractId.toString());
    }

    // ========== Status-Übergänge ==========

    @Test
    @DisplayName("Vertrag kann aktiviert werden")
    void activateContract_success() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(validContract));
        when(contractRepository.save(any())).thenReturn(validContract);

        Contract result = contractService.activateContract(contractId);

        assertThat(validContract.getContractStatus()).isEqualTo(ContractStatus.ACTIVE);
        verify(contractRepository, times(1)).save(validContract);
    }

    @Test
    @DisplayName("Aktivieren eines nicht vorhandenen Vertrags wirft ResourceNotFoundException")
    void activateContract_notFound_throwsResourceNotFoundException() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.activateContract(contractId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ========== getContractById ==========

    @Test
    @DisplayName("getContractById gibt Vertrag zurück wenn vorhanden")
    void getContractById_found() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(validContract));

        Optional<Contract> result = contractService.getContractById(contractId);

        assertThat(result).isPresent();
        assertThat(result.get().getContractTitle()).isEqualTo("Testvertrag");
    }

    @Test
    @DisplayName("getContractById gibt leeres Optional zurück wenn nicht vorhanden")
    void getContractById_notFound() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        assertThat(contractService.getContractById(contractId)).isEmpty();
    }
}
