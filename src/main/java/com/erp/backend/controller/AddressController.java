package com.erp.backend.controller;

import com.erp.backend.domain.Address;
import com.erp.backend.dto.AddressDTO;
import com.erp.backend.service.AddressService;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public List<AddressDTO> getAll() {
        return service.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressDTO> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(addr -> ResponseEntity.ok(toDTO(addr)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/init")
    public ResponseEntity<String> initTestAddresses() {
        service.initTestAddresses();
        return ResponseEntity.ok("15 Testadressen wurden erstellt.");
    }

    @PostMapping
    public ResponseEntity<AddressDTO> create(@RequestBody AddressDTO dto) {
        Address saved = service.save(fromDTO(dto));
        return ResponseEntity.ok(toDTO(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Address> update(@PathVariable Long id, @RequestBody AddressDTO dto) {
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


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
