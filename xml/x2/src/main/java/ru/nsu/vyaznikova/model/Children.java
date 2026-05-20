package ru.nsu.vyaznikova.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Children {
    @XmlElement(name = "son")
    private List<ChildRefOrName> son = new ArrayList<>();

    @XmlElement(name = "daughter")
    private List<ChildRefOrName> daughter = new ArrayList<>();

    public List<ChildRefOrName> getSon() {
        return son;
    }

    public void setSon(List<ChildRefOrName> son) {
        this.son = son;
    }

    public List<ChildRefOrName> getDaughter() {
        return daughter;
    }

    public void setDaughter(List<ChildRefOrName> daughter) {
        this.daughter = daughter;
    }
}
