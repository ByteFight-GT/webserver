package com.example.botfightwebserver;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

@Entity
@RevisionEntity
@Table(name = "REVINFO")
public class CustomRevisionEntity extends DefaultRevisionEntity {
    // Using default implementation
}