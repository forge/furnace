/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.arquillian.impl.FurnaceDeploymentScenarioGenerator;

/**
 * Deploy this addon as a local service.
 * 
 * It basically calls {@link AddonArchive#addAsLocalServices(Class...)} for the class under test in
 * {@link FurnaceDeploymentScenarioGenerator} when no {@link Deployment} methods are found
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DeployAsLocalService
{
}
