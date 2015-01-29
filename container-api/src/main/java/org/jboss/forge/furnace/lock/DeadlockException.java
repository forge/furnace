package org.jboss.forge.furnace.lock;

import org.jboss.forge.furnace.exception.ContainerException;

/**
 * Thrown when a deadlock is detected in the {@link LockManager}. It is intentional that this type does not extend from
 * {@link ContainerException} because it represents a developer error, and should NEVER be caught. (This is the reason
 * why it extends from {@link Error})
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class DeadlockException extends Error
{
    private static final long serialVersionUID = -5559271290352031320L;

    /**
     * Create a new {@link DeadlockException} with the given message.
     */
    public DeadlockException(String string)
    {
        super(string);
    }
}