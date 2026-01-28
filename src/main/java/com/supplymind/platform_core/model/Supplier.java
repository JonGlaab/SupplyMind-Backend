package com.supplymind.platform_core.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 255)
    @Column(name = "contact_email")
    private String contactEmail;

    @Size(max = 20)
    @Column(name = "phone", length = 20)
    private String phone;


    @Column(name = "address")
    private String address;

    @OneToMany(mappedBy = "supplier")
    private Set<PurchaseOrder> purchaseOrders = new LinkedHashSet<>();

    @OneToMany(mappedBy = "supplier")
    private Set<SupplierProduct> supplierProducts = new LinkedHashSet<>();

}