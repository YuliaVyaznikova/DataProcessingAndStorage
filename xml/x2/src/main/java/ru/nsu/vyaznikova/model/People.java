package ru.nsu.vyaznikova.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "people")
@XmlAccessorType(XmlAccessType.FIELD)
public class People {
    @XmlAttribute(name = "uniqueCount")
    private Integer uniqueCount;

    @XmlElement(name = "person")
    private List<Person> person = new ArrayList<>();

    public Integer getUniqueCount() {
        return uniqueCount;
    }

    public void setUniqueCount(Integer uniqueCount) {
        this.uniqueCount = uniqueCount;
    }

    public List<Person> getPerson() {
        return person;
    }

    public void setPerson(List<Person> person) {
        this.person = person;
    }
}
