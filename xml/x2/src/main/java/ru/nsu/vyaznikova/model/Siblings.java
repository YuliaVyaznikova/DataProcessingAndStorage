package ru.nsu.vyaznikova.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Siblings {
    @XmlElement(name = "brother")
    private List<SiblingRef> brother = new ArrayList<>();

    @XmlElement(name = "sister")
    private List<SiblingRef> sister = new ArrayList<>();

    public List<SiblingRef> getBrother() {
        return brother;
    }

    public void setBrother(List<SiblingRef> brother) {
        this.brother = brother;
    }

    public List<SiblingRef> getSister() {
        return sister;
    }

    public void setSister(List<SiblingRef> sister) {
        this.sister = sister;
    }
}
