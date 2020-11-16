package net.odbogm;

/**
 *
 * @author jbertinetti
 */
public class Config {
    
    public static class OdbogmGlobalConfig {
        
        public static boolean EQUALS_AND_HASHCODE_TRIGGER_LOAD_LAZY_LINKS = true;
        public static boolean EQUALS_AND_HASHCODE_ON_DELETED_THROWS_EXCEPTION = true;
        public static boolean AUDITOR_CREATES_AUDIT_SCHEMA = true;
        
    }
    
    
    private boolean equalsAndHashCodeTriggerLoadLazyLinks = OdbogmGlobalConfig.EQUALS_AND_HASHCODE_TRIGGER_LOAD_LAZY_LINKS;
    
    private boolean equalsAndHashCodeOnDeletedThrowsException = OdbogmGlobalConfig.EQUALS_AND_HASHCODE_ON_DELETED_THROWS_EXCEPTION;

    private boolean auditorCreatesAuditSchema = OdbogmGlobalConfig.AUDITOR_CREATES_AUDIT_SCHEMA;

    
    public Config() {
    }

    public boolean isEqualsAndHashCodeTriggerLoadLazyLinks() {
        return equalsAndHashCodeTriggerLoadLazyLinks;
    }

    public void setEqualsAndHashCodeTriggerLoadLazyLinks(boolean equalsAndHashCodeTriggerLoadLazyLinks) {
        this.equalsAndHashCodeTriggerLoadLazyLinks = equalsAndHashCodeTriggerLoadLazyLinks;
    }

    public boolean isEqualsAndHashCodeOnDeletedThrowsException() {
        return equalsAndHashCodeOnDeletedThrowsException;
    }

    public void setEqualsAndHashCodeOnDeletedThrowsException(boolean equalsAndHashCodeOnDeletedThrowsException) {
        this.equalsAndHashCodeOnDeletedThrowsException = equalsAndHashCodeOnDeletedThrowsException;
    }

    public boolean isAuditorCreatesAuditSchema() {
        return auditorCreatesAuditSchema;
    }

    public void setAuditorCreatesAuditSchema(boolean auditorCreatesAuditSchema) {
        this.auditorCreatesAuditSchema = auditorCreatesAuditSchema;
    }
    
}
