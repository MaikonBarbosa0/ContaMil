package br.com.contamil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
class Category {
    @Id
    Long id;

    @Column(nullable = false, unique = true, length = 80, updatable = false)
    String name;

    @Column(nullable = false, length = 7)
    String color;

    protected Category() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
