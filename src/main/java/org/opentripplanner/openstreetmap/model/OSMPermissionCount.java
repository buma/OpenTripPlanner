package org.opentripplanner.openstreetmap.model;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.module.osm.OSMSpecifier;
import org.opentripplanner.graph_builder.module.osm.WayPropertyPicker;
import org.opentripplanner.osm.Way;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by mabu on 25.5.2015.
 */
public class OSMPermissionCount {

    private List<WayPropertyPicker> leftMixins;

    private List<WayPropertyPicker> rightMixins;

    private int bestLeftScore;

    private int bestRightScore;

    private WayPropertyPicker leftPicker;

    private WayPropertyPicker rightPicker;

    P2<Double> finalSafetyValues;

    public OSMPermissionCount() {
        bestLeftScore = 0;
        bestRightScore = 0;
        leftPicker = null;
        rightPicker = null;

        leftMixins = new ArrayList<>(10);
        rightMixins = new ArrayList<>(10);
    }

    private List<String> getMixin(List<WayPropertyPicker> mixin) {
        List<String> mixinStrings = new ArrayList<>(mixin.size());
        for(WayPropertyPicker picker: mixin) {
            String mix = mixinToString(picker);
            mixinStrings.add(mix);
        }
        return mixinStrings;
    }

    private String mixinToString(WayPropertyPicker picker) {
        if (picker == null) {
            return null;
        }
        //return picker.getSpecifier().toString() + "[" + picker.getProperties().getPermission() + "," + picker.getProperties().getSafetyFeatures() + "]";
        return String.format(Locale.US, "%s[%s,P2(%.2f, %.2f)]", picker.getSpecifier(),
            picker.getProperties().getPermission(), picker.getProperties().getSafetyFeatures().first,
            picker.getProperties().getSafetyFeatures().second);
    }

    public List<String> getLeftMixins() {
        return Collections.unmodifiableList(getMixin(leftMixins));
    }

    public List<String> getRightMixins() {
        return Collections.unmodifiableList(getMixin(rightMixins));
    }

    public String getLeftPicker() {
        return mixinToString(leftPicker);
    }

    public String getRightPicker() {
        return mixinToString(rightPicker);
    }

    public boolean hasLeftMixins() {
        return !leftMixins.isEmpty();
    }

    public boolean hasRightMixins() {
        return !rightMixins.isEmpty();
    }

    public void addLeftMixins(WayPropertyPicker picker) {
        leftMixins.add(picker);
    }

    public void addRightMixins(WayPropertyPicker picker) {
        rightMixins.add(picker);
    }

    public void addLeftSpecifier(WayPropertyPicker picker, int leftScore) {
        bestLeftScore = leftScore;
        leftPicker = picker;
    }

    public void addRightSpecifier(WayPropertyPicker picker, int rightScore) {
        bestRightScore = rightScore;
        rightPicker = picker;
    }

    private String mixinToString(List<WayPropertyPicker> mixin, boolean right) {
        if (mixin.isEmpty()) {
            return "";
        }
        StringBuilder stringBuilder;
        if (right) {
            stringBuilder = new StringBuilder("right: ");
        } else {
            stringBuilder = new StringBuilder("left: ");
        }
        for (WayPropertyPicker picker: mixin) {
            P2<Double> safetyFeatures = picker.getProperties().getSafetyFeatures();
            double safetyValue;

            if (right) {
                safetyValue = safetyFeatures.second;
            } else {
                safetyValue = safetyFeatures.first;
            }
            stringBuilder.append(picker.getSpecifier().toString());
            stringBuilder.append("[");
            stringBuilder.append(picker.getProperties().getPermission());
            stringBuilder.append(",");
            stringBuilder.append(safetyValue);
            stringBuilder.append("]");
            stringBuilder.append("*");
        }
        stringBuilder.setLength(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        String perm = "Permissions: ";

        if (rightPicker != null) {
            perm += rightPicker.getSpecifier().toString() + "[" + rightPicker.getProperties()
                .getPermission();
            if (leftPicker != null) {
                perm += "," + rightPicker.getProperties().getSafetyFeatures().first + "|" + leftPicker
                    .getProperties().getSafetyFeatures().second;
            }
            perm += "]";
        }

        perm += mixinToString(leftMixins, false);

        perm += mixinToString(rightMixins, true);

        perm += " FS:" + finalSafetyValues;

        return perm;
    }

    public void addFinalSafetyFeatures(P2<Double> safetyFeatures) {
        finalSafetyValues = safetyFeatures;
    }
}
