package com.skalez.product.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_translations", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "language"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;
    @Column(nullable = false, length = 10)
    private String language;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(length = 2000)
    private String description;
    ProductTranslation(Product product, String language, String name, String description){
        this.product = product;
        this.language = language;
        this.name = name;
        this.description = description;
    }

}
