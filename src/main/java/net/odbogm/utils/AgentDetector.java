package net.odbogm.utils;

import net.odbogm.annotations.Entity;

/**
 * This class only serves to check if the ODBOGM Agent is already loaded. All
 * classes annotated with @Entity are instances of ITransparentDirtyDetector if
 * the agent is active.
 * 
 * @author jbertinetti
 */
@Entity
public class AgentDetector {
}
