package org.springframework.roo.process.manager;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.event.ProcessManagerStatus;
import org.springframework.roo.shell.ExecutionStrategy;
import org.springframework.roo.shell.ParseResult;

/**
 * Used to dispatch shell {@link ExecutionStrategy} requests through
 * {@link ProcessManager#execute(org.springframework.roo.process.manager.CommandCallback)}
 * .
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
@Reference(name = "processManager", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = ProcessManager.class, cardinality = ReferenceCardinality.MANDATORY_UNARY)
public class ProcessManagerHostedExecutionStrategy implements ExecutionStrategy {

    private final Class<?> mutex = ProcessManagerHostedExecutionStrategy.class;
    private ProcessManager processManager;

    protected void bindProcessManager(final ProcessManager processManager) {
        synchronized (mutex) {
            this.processManager = processManager;
        }
    }

    public Object execute(final ParseResult parseResult)
            throws RuntimeException {
        Validate.notNull(parseResult, "Parse result required");
        synchronized (mutex) {
            Validate.isTrue(isReadyForCommands(),
                    "ProcessManagerHostedExecutionStrategy not yet ready for commands");
            return processManager.execute(new CommandCallback<Object>() {
                public Object callback() {
                    try {
                        return parseResult.getMethod().invoke(
                                parseResult.getInstance(),
                                parseResult.getArguments());
                    }
                    catch (Exception e) {
                        throw new RuntimeException(ObjectUtils.defaultIfNull(
                                ExceptionUtils.getRootCause(e), e));
                    }
                }
            });
        }
    }

    public boolean isReadyForCommands() {
        synchronized (mutex) {
            if (processManager != null) {
                // BUSY_EXECUTION needed in case of recursive commands, such as
                // if executing a script
                // TERMINATED added in case of additional commands following a
                // quit or exit in a script - ROO-2270
                final ProcessManagerStatus processManagerStatus = processManager
                        .getProcessManagerStatus();
                return processManagerStatus == ProcessManagerStatus.AVAILABLE
                        || processManagerStatus == ProcessManagerStatus.BUSY_EXECUTING
                        || processManagerStatus == ProcessManagerStatus.TERMINATED;
            }
        }
        return false;
    }

    public void terminate() {
        synchronized (mutex) {
            if (processManager != null) {
                processManager.terminate();
            }
        }
    }

    protected void unbindProcessManager(final ProcessManager processManager) {
        synchronized (mutex) {
            this.processManager = null;
        }
    }
}
