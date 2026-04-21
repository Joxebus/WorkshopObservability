package io.github.joxebus.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*
import jakarta.validation.constraints.*

@Entity
@Table(name = "person")
@JsonIgnoreProperties(["hibernateLazyInitializer", "handler"])
class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @Column(nullable = false, length = 30)
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 30, message = "Name must be between 1 and 30 characters")
    String name

    @Column(nullable = false, length = 30)
    @NotBlank(message = "Lastname is required")
    @Size(min = 1, max = 30, message = "Lastname must be between 1 and 30 characters")
    String lastname

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    String email

    @Override
    String toString() {
        "Person{id=$id, name='$name', lastname='$lastname', email='$email'}"
    }
}
