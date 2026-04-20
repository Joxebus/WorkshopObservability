package io.github.joxebus.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.gorm.annotation.Entity


@Entity
@JsonIgnoreProperties(["errors","attached", "dirtyPropertyNames", "dirty"])
class Person {

    String name
    String lastname
    String email

    static constraints = {
        name size:1..30, blank: false, nullable: false
        lastname size:1..30, blank: false, nullable: false
        email email:true, blank: false, nullable: false
    }


    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", lastname='" + lastname + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
