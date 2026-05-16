package com.erp.backend.controller;

import com.erp.backend.domain.Address;
import com.erp.backend.dto.AddressDTO;
import com.erp.backend.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/addresses")
@CrossOrigin
public class AddressController {

    private final AddressService service;

    public AddressController(AddressService service) {
        this.service = service;
    }

    // --- Hilfsmethode für DTO Mapping ---
    private AddressDTO toDTO(Address address) {
        return new AddressDTO(address.getId(), address.getStreet(), address.getPostalCode(),
                address.getCity(), address.getCountry());
    }

    private Address fromDTO(AddressDTO dto) {
        Address addr = new Address();
        addr.setStreet(dto.getStreet());
        addr.setPostalCode(dto.getPostalCode());
        addr.setCity(dto.getCity());
        addr.setCountry(dto.getCountry());
        return addr;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'ADDRESSES_READ')")
    @GetMapping
    public ResponseEntity<List<AddressDTO>> getAll(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "city") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        if (paginated) {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Address> addressPage = service.findAll(pageable);
            List<AddressDTO> dtos = addressPage.getContent().stream().map(this::toDTO).toList();
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(addressPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(addressPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(dtos);
        }
        return ResponseEntity.ok(service.findAll().stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'ADDRESSES_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<AddressDTO> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(addr -> ResponseEntity.ok(toDTO(addr)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/init")
    public ResponseEntity<String> initTestAddresses() {
        service.initTestAddresses();
        return ResponseEntity.ok("15 Testadressen wurden erstellt.");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AddressDTO> create(@Valid @RequestBody AddressDTO dto) {
        Address saved = service.save(fromDTO(dto));
        return ResponseEntity.ok(toDTO(saved));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Address> update(@PathVariable Long id, @Valid @RequestBody AddressDTO dto) {
        Optional<Address> existing = service.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Address addr = existing.get();
        addr.setStreet(dto.getStreet());
        addr.setPostalCode(dto.getPostalCode());
        addr.setCity(dto.getCity());
        addr.setCountry(dto.getCountry());

        Address updated = service.save(addr);
        return ResponseEntity.ok(updated);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
