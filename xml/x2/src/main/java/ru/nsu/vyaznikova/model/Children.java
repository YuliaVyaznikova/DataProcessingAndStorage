package ru.nsu.vyaznikova.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Children {
    @XmlElement(name = "son")
    private List<ChildRef> son = new ArrayList<>();

    @XmlElement(name = "daughter")
    private List<ChildRef> daughter = new ArrayList<>();

    public List<ChildRef> getSon() {
        return son;
    }

    public void setSon(List<ChildRef> son) {
        this.son = son;
    }

    public List<ChildRef> getDaughter() {
        return daughter;
    }

    public void setDaughter(List<ChildRef> daughter) {
        this.daughter = daughter;
    }
}
