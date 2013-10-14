package org.jboss.forge.arquillian.archive;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface RepositorySelector
{
   String getAddonRepository();

   RepositorySelector setAddonRepository(String repository);
}
