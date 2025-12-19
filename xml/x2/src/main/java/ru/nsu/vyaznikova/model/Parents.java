package ru.nsu.vyaznikova.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Parents {
    @XmlElement(name = "father")
    private List<ParentRefOrName> father = new ArrayList<>();

    @XmlElement(name = "mother")
    private List<ParentRefOrName> mother = new ArrayList<>();

    public List<ParentRefOrName> getFather() {
        return father;
    }

    public void setFather(List<ParentRefOrName> father) {
        this.father = father;
    }

    public List<ParentRefOrName> getMother() {
        return mother;
    }

    public void setMother(List<ParentRefOrName> mother) {
        this.mother = mother;
    }
}
