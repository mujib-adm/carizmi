package org.sofumar.portal.framework.vo;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.framework.data.msg.FieldMessage;
import org.sofumar.portal.framework.data.msg.Message;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class ValueObject {

    @CreatedBy
    @Column(name = FieldConstants.CREATED_BY, nullable = false, updatable = false)
    protected String createdBy;

    @LastModifiedBy
    @Column(name = FieldConstants.LAST_MODIFIED_BY)
    protected String lastModifiedBy;

    @CreatedDate
    @Column(name = FieldConstants.CREATE_DATE_TIME, nullable = false, updatable = false)
    protected LocalDateTime createdDateTime;

    @LastModifiedDate
    @Column(name = FieldConstants.LAST_MODIFIED_DATE_TIME)
    protected LocalDateTime lastModifiedDateTime;

    @Transient
    private boolean errorExists;

    @Transient
    private boolean warningExists;

    @Transient
    private LinkedHashSet<Message> globalMsgSet = new LinkedHashSet<>();

    @Transient
    private LinkedHashMap<String, List<FieldMessage>> fieldMsgMap = new LinkedHashMap<>();


    public abstract String getTableName();

    public boolean hasErrors() {
        return errorExists;
    }

    public boolean hasWarnings() {
        return warningExists;
    }

    public Set<Message> getGlobalMessages() {
        return Collections.unmodifiableSet(globalMsgSet);
    }

    public Map<String, List<FieldMessage>> getFieldMessages() {
        return Collections.unmodifiableMap(fieldMsgMap);
    }

    public void addGlobalMessage(Message message) {
        this.globalMsgSet.add(message);
        setErrorWarningFlags(message.getType());
    }

    public void addFieldMessage(String field, Message message) {
        FieldMessage fieldMessage = new FieldMessage(field, message);
        setFieldMessages(field, fieldMessage);
    }

    private List<FieldMessage> getFieldMessages(String field, Message message) {
        List<FieldMessage> fieldMessages = new ArrayList<>();
        if (message != null) {
            fieldMessages.add(new FieldMessage(field, message));
        }
        return fieldMessages;
    }

    private void setFieldMessages(String field, FieldMessage message) {
        List<FieldMessage> fieldMsgs = fieldMsgMap.get(field);
        if (fieldMsgs != null) {
            fieldMsgs.add(message);
        } else {
            fieldMsgMap.put(field, List.of(message));
        }
        setFlags(message);
    }

    private void setFlags(FieldMessage fieldMessage) {
        if (isErrorWarningFlagsSet()) {
            return;
        }
        setErrorWarningFlags(fieldMessage.message.getType());
    }

    private void setErrorWarningFlags(Message.Type type) {
        switch (type) {
            case ERROR -> this.errorExists = true;
            case WARNING -> this.warningExists = true;
        }
    }

    private boolean isErrorWarningFlagsSet() {
        return this.errorExists && this.warningExists;
    }

    // Utility method to get field value by name using reflection
    public Object getFieldValue(String fieldName) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(this);
        } catch (Exception e) {
            return "unknown";
        }
    }

}
