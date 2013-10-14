/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian;

import org.jboss.arquillian.container.test.api.ShouldThrowException;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public enum Strategy
{
   /**
    * {@link AddonDependency} instances are deployed and enabled one at a time, and deployment success or failure is
    * reported during deployment. This {@link Strategy} should be used when addon deployment order or deployment
    * verification via {@link ShouldThrowException} is required.
    */
   ISOLATED,

   /**
    * {@link AddonDependency} instances are deployed one at a time, but enabled as a group. This strategy results in
    * faster tests, but should be used when a simple success or failure result is acceptable.
    */
   AGGREGATE
}
