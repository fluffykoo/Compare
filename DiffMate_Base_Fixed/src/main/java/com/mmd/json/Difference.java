package com.mmd.json;

public class Difference {
    private final String entityId;
    private final ChangeType type;
    private final String section;
    private final String key;
    private final String oldValue;
    private final String newValue;

    public Difference(String entityId, ChangeType type, String section, String key,
                      String oldValue, String newValue) {
        this.entityId = entityId;
        this.type = type;
        this.section = section;
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getEntityId() { return entityId; }
    public ChangeType getType() { return type; }
    public String getSection() { return section; }
    public String getKey() { return key; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }

    @Override
    public String toString() {
        return "Difference{" +
                "entityId='" + entityId + '\'' +
                ", type=" + type +
                ", section='" + section + '\'' +
                ", key='" + key + '\'' +
                ", oldValue='" + oldValue + '\'' +
                ", newValue='" + newValue + '\'' +
                '}';
    }
}
