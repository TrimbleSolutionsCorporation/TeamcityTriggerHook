package com.atlassian.ecohelp;

import com.atlassian.bitbucket.i18n.I18nService;
import com.atlassian.bitbucket.project.ProjectService;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.hook.repository.RepositoryHookService;
import com.atlassian.sal.api.ApplicationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.atlassian.bitbucket.rest.v2.api.resolver.RepositoryResolver;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import static com.atlassian.plugins.osgi.javaconfig.OsgiServices.importOsgiService;


@Configuration
public class Application {

    @Bean
    public I18nService i18nService() {
        return importOsgiService(I18nService.class);
    }

    @Bean
    public RepositoryService repositoryService() {
        return importOsgiService(RepositoryService.class);
    }


    @Bean
    public CommitService commitService() {
        return importOsgiService(CommitService.class);
    }

    @Bean
    public ProjectService projectService() {
        return importOsgiService(ProjectService.class);
    }    
    @Bean
    public ApplicationProperties applicationProperties() {
        return importOsgiService(ApplicationProperties.class);
    }

    @Bean
    public RepositoryResolver repositoryResolver() {
        return importOsgiService(RepositoryResolver.class);
    }        
}
