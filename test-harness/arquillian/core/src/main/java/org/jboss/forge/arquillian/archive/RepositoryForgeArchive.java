package org.jboss.forge.arquillian.archive;

/**
 * Archive representing a Furnace AddonDependency deployment to be installed into a specific repository.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface RepositoryForgeArchive extends ForgeArchive
{
   String getAddonRepository();
   
   RepositoryForgeArchive setAddonRepository(String repository);
}
