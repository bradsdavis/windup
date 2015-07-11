package org.jboss.windup.exec.configuration.options;

import org.jboss.windup.config.AbstractConfigurationOption;
import org.jboss.windup.config.InputType;
import org.jboss.windup.config.ValidationResult;

/**
 * Indicates that all operations should function in "Offline" mode (without accessing the internet).
 */
public class AnalyticsOption extends AbstractConfigurationOption
{
    public static final String NAME = "analytics";

    @Override
    public String getDescription()
    {
        return "Indicates whether to send anonymous migration analytics to Red Hat";
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getLabel()
    {
        return "Application Analytics";
    }

    @Override
    public Class<?> getType()
    {
        return Boolean.class;
    }

    @Override
    public InputType getUIType()
    {
        return InputType.SINGLE;
    }

    @Override
    public boolean isRequired()
    {
        return false;
    }

    public ValidationResult validate(Object valueObj)
    {
        return ValidationResult.SUCCESS;
    }
}
