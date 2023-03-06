package br.hendrew;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import br.hendrew.api.rest.bindings.GranteeCapabilitiesType;
import br.hendrew.api.rest.bindings.GroupType;
import br.hendrew.api.rest.bindings.ProjectListType;
import br.hendrew.api.rest.bindings.ProjectType;
import br.hendrew.api.rest.bindings.TableauCredentialsType;
import br.hendrew.api.rest.bindings.WorkbookType;
import br.hendrew.api.rest.util.RestApiUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@RequestScoped
public class IndicadoresService {

    private RestApiUtils s_restApiUtils;
    private Logger s_logger;
    private Properties s_properties;

    public void iniciar() {
        s_logger = Logger.getLogger(IndicadoresService.class);
        s_properties = new Properties();
        s_restApiUtils = RestApiUtils.getInstance();
        BasicConfigurator.configure();

        // Loads the values from configuration file into the Properties instance
        try {
            s_properties.load(new FileInputStream("C:/indicadores/src/main/java/br/hendrew/res/config.properties"));
        } catch (IOException e) {
            s_logger.error("Failed to load configuration files.");
        }
    }

    public TableauCredentialsType logar() {
        String username = s_properties.getProperty("user.admin.name");
        String password = s_properties.getProperty("user.admin.password");
        String contentUrl = s_properties.getProperty("site.default.contentUrl");
        // Signs in to server and saves the authentication token, site ID, and current user ID
        TableauCredentialsType credential = s_restApiUtils.invokeSignIn(username, password, contentUrl);
        String currentSiteId = credential.getSite().getId();
        String currentUserId = credential.getUser().getId();

        s_logger.info(String.format("Authentication token: %s", credential.getToken()));
        s_logger.info(String.format("Site ID: %s", currentSiteId));
        return credential;
    }

    public TableauCredentialsType montarResto() {

        String username = s_properties.getProperty("user.admin.name");
        String password = s_properties.getProperty("user.admin.password");
        String contentUrl = s_properties.getProperty("site.default.contentUrl");
        // Signs in to server and saves the authentication token, site ID, and current user ID
        TableauCredentialsType credential = s_restApiUtils.invokeSignIn(username, password, contentUrl);
        String currentSiteId = credential.getSite().getId();
        String currentUserId = credential.getUser().getId();

        s_logger.info(String.format("Authentication token: %s", credential.getToken()));
        s_logger.info(String.format("Site ID: %s", currentSiteId));

        // Queries the projects on the current site and iterates over the list to
        // find the ID of the default project
        ProjectType defaultProject = null;
        ProjectListType projects = s_restApiUtils.invokeQueryProjects(credential, currentSiteId);
        for (ProjectType project : projects.getProject()) {
            if (project.getName().equals("default") || project.getName().equals("Default")) {
                defaultProject = project;

                s_logger.info(String.format("Default project found: %s", defaultProject.getId()));
            }
        }

        // Ensure the default project was found before attempting to use it; if it was not found,
        // log the failure and exit gracefully
        if (defaultProject == null) {
            s_logger.error("Failed to find default project");

            // Signs out of the server. This invalidates the authentication token so
            // that it cannot be used for more requests.
            s_restApiUtils.invokeSignOut(credential);

            s_logger.info("Exiting without publishing due to previous failure");
            return null;
        }

        // Sets the name to assign to the workbook to be published
        String workbookName = s_properties.getProperty("workbook.sample.name");

        // Gets the workbook file to publish
        String workbookPath = s_properties.getProperty("workbook.sample.path");
        File workbookFile = new File(workbookPath);

        // Gets whether or not to publish the workbook using file uploads
        boolean chunkedPublish = Boolean.valueOf(s_properties.getProperty("workbook.publish.chunked"));

        // Publishes the workbook as a multipart request
        WorkbookType publishedWorkbook = s_restApiUtils.invokePublishWorkbook(credential, currentSiteId,
                defaultProject.getId(), workbookName, workbookFile, chunkedPublish);

        // Creates a non Active Directory group named "TableauExample"
        GroupType group = s_restApiUtils.invokeCreateGroup(credential, currentSiteId, "TableauExample");

        // Sets permission to allow the group to read the new workbook, but not
        // to modify its permissions
        Map<String, String> capabilities = new HashMap<>();
        capabilities.put("Read", "Allow");
        capabilities.put("ChangePermissions", "Deny");

        // Creates the grantee capability element for the group
        GranteeCapabilitiesType groupCapabilities = s_restApiUtils.createGroupGranteeCapability(group, capabilities);

        // Adds the created group to the list of grantees
        List<GranteeCapabilitiesType> granteeCapabilities = new ArrayList<>();
        granteeCapabilities.add(groupCapabilities);

        // Makes the call to add the permissions
        s_restApiUtils.invokeAddPermissionsToWorkbook(credential, currentSiteId, publishedWorkbook.getId(),
                granteeCapabilities);

        // Gets the list of workbooks the current user can read
        List<WorkbookType> currentUserWorkbooks = s_restApiUtils.invokeQueryWorkbooks(credential, currentSiteId,
                currentUserId).getWorkbook();

        // Checks whether the workbook published previously is in the list, then
        // checks whether the workbook's owner is the current user
        for (WorkbookType workbook : currentUserWorkbooks) {
            if (workbook.getId().equals(publishedWorkbook.getId())) {
                s_logger.debug(String.format("Published workbook found: %s", workbook.getId()));

                if (workbook.getOwner().getId().equals(currentUserId)) {
                    s_logger.debug("Published workbook was published by current user");
                }
            }
        }

        // Signs out of the server. This invalidates the authentication token so
        // that it cannot be used for more requests.
        s_restApiUtils.invokeSignOut(credential);

        return credential;

    }

}
