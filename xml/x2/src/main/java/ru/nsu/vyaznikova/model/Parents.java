package ru.nsu.vyaznikova.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Parents {
    @XmlElement(name = "father")
    private ParentRefOrName father;

    @XmlElement(name = "mother")
    private ParentRefOrName mother;

    public ParentRefOrName getFather() {
        return father;
    }

    public void setFather(ParentRefOrName father) {
        this.father = father;
    }

    public ParentRefOrName getMother() {
        return mother;
    }

    public void setMother(ParentRefOrName mother) {
        this.mother = mother;
    }
}