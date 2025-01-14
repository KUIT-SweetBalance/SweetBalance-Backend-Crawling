package com.sweetbalance.crawling_component.entity;

import com.sweetbalance.crawling_component.enums.beverage.BeverageCategory;
import com.sweetbalance.crawling_component.enums.common.Status;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "beverages")
@NoArgsConstructor
@AllArgsConstructor
@Builder @Getter @Setter
public class Beverage extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long beverageId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(name = "img_url", length = 500)
    private String imgUrl;

    @Enumerated(EnumType.STRING)
    private BeverageCategory category;

    @Column(nullable = false)
    private double sugar;

    @Column(nullable = false)
    private double calories;

    @Column(nullable = false)
    private double caffeine;

    @Enumerated(EnumType.STRING)
    @Column
    private Status status;

    @Builder.Default
    @OneToMany(mappedBy = "beverage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BeverageSize> sizes = new ArrayList<>();

    public void addSize(BeverageSize size) {
        sizes.add(size);
        size.setBeverage(this);
    }

    public void removeSize(BeverageSize size) {
        sizes.remove(size);
        size.setBeverage(null);
    }
}

