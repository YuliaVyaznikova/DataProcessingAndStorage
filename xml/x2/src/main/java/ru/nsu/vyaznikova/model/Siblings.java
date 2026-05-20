package ru.nsu.vyaznikova.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Siblings {
    @XmlElement(name = "brother")
    private List<SiblingRefOrName> brother = new ArrayList<>();

    @XmlElement(name = "sister")
    private List<SiblingRefOrName> sister = new ArrayList<>();

    public List<SiblingRefOrName> getBrother() {
        return brother;
    }

    public void setBrother(List<SiblingRefOrName> brother) {
        this.brother = brother;
    }

    public List<SiblingRefOrName> getSister() {
        return sister;
    }

    public void setSister(List<SiblingRefOrName> sister) {
        this.sister = sister;
    }
}
