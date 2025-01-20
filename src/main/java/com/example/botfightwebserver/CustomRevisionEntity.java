package com.example.botfightwebserver;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;
import org.springframework.context.annotation.Configuration;

@Entity
@Table(name = "REVINFO")
public class RevisionEntity extends DefaultRevisionEntity {
    // Using default implementation
}