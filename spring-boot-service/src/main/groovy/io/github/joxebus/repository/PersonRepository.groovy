package io.github.joxebus.repository

import io.github.joxebus.domain.Person
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonRepository extends JpaRepository<Person, Long> {
    // JpaRepository provides all basic CRUD operations
    // findAll(), findById(), save(), delete(), etc.
}
