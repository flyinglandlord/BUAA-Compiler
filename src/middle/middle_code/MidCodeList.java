package middle.middle_code;

import middle.middle_code.element.MidCode;

import java.util.ArrayList;
import java.util.List;

public class MidCodeList {
    private final List<MidCode> midCodeList = new ArrayList<>();

    public MidCodeList() {
    }

    public void addMidCode(MidCode midCode) {
        midCodeList.add(midCode);
    }

    public void insertBefore(MidCode midCode, MidCode target) {
        midCodeList.add(midCodeList.indexOf(target), midCode);
    }

    public void insertBefore(int index, MidCode midCode) {
        midCodeList.add(index, midCode);
    }

    public void insertAfter(MidCode midCode, MidCode target) {
        midCodeList.add(midCodeList.indexOf(target) + 1, midCode);
    }

    public void insertAfter(int index, MidCode midCode) {
        midCodeList.add(index + 1, midCode);
    }

    public void replace(MidCode midCode, MidCode target) {
        if (!midCodeList.contains(midCode)) return;
        midCodeList.set(midCodeList.indexOf(midCode), target);
    }

    public void replace(int index, MidCode midCode) {
        midCodeList.set(index, midCode);
    }

    public void remove(MidCode midCode) {
        midCodeList.remove(midCode);
    }

    public void remove(int index) {
        midCodeList.remove(index);
    }

    public MidCode get(int index) {
        return midCodeList.get(index);
    }

    public int size() {
        return midCodeList.size();
    }

    public List<MidCode> getMidCodeList() {
        return midCodeList;
    }
}
