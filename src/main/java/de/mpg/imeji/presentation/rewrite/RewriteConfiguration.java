package de.mpg.imeji.presentation.rewrite;

import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.servlet.config.HttpConfigurationProvider;
import org.ocpsoft.rewrite.servlet.config.rule.Join;

import jakarta.servlet.ServletContext;

@org.ocpsoft.rewrite.annotation.RewriteConfiguration
public class RewriteConfiguration extends HttpConfigurationProvider {
  @Override
  public Configuration getConfiguration(ServletContext servletContext) {
    return ConfigurationBuilder.begin().addRule(Join.path("/").to("/jsf/StartPage.xhtml"))

        .addRule(Join.path("/browse").to("/jsf/Items.xhtml")).addRule(Join.path("/edit/license").to("/jsf/ItemsEditLicense.xhtml"))
        .addRule(Join.path("/editselected").to("/jsf/EditItemsSelected.xhtml"))
        .addRule(Join.path("/editbatch").to("/jsf/EditItemsBatch.xhtml")).addRule(Join.path("/item/{id}").to("/jsf/Item.xhtml"))
        .addRule(Join.path("/upload").to("/jsf/Upload.xhtml"))

        .addRule(Join.path("/collections").to("/jsf/Collections.xhtml"))
        .addRule(Join.path("/collection/{collectionId}").to("/jsf/CollectionBrowse.xhtml"))
        .addRule(Join.path("/collection/{collectionId}/item/{id}").to("/jsf/CollectionImage.xhtml"))
        .addRule(Join.path("/collection/{collectionId}/edit").to("/jsf/CollectionEdit.xhtml"))
        .addRule(Join.path("/createcollection").to("/jsf/CollectionCreate.xhtml"))
        .addRule(Join.path("/collection/{collectionId}/share").to("/jsf/Share.xhtml"))

        .addRule(Join.path("/createstatement").to("/jsf/StatementCreate.xhtml"))
        .addRule(Join.path("/statement/{statementId}/edit").to("/jsf/StatementEdit.xhtml"))
        .addRule(Join.path("/statement/edit").to("/jsf/StatementEdit.xhtml")).addRule(Join.path("/statements").to("/jsf/Statements.xhtml"))

        .addRule(Join.path("/createfacet").to("/jsf/FacetCreate.xhtml"))
        .addRule(Join.path("/facet/{facetId}/edit").to("/jsf/FacetEdit.xhtml")).addRule(Join.path("/facets").to("/jsf/Facets.xhtml"))

        .addRule(Join.path("/search").to("/jsf/SearchAdvanced.xhtml"))

        .addRule(Join.path("/admin").to("/jsf/AdminOverview.xhtml")).addRule(Join.path("/admin/config").to("/jsf/AdminConfig.xhtml"))
        .addRule(Join.path("/admin/config/mailserver").to("/jsf/AdminConfigEMailServer.xhtml"))
        .addRule(Join.path("/admin/config/doiservice").to("/jsf/AdminConfigDoiService.xhtml"))
        .addRule(Join.path("/admin/config/collections").to("/jsf/AdminConfigCollections.xhtml"))
        .addRule(Join.path("/admin/tools").to("/jsf/AdminTools.xhtml"))
        .addRule(Join.path("/admin/statistics").to("/jsf/AdminStatistics.xhtml"))
        .addRule(Join.path("/admin/storageusage").to("/jsf/AdminStorageUsage.xhtml"))

        .addRule(Join.path("/createuser").to("/jsf/UserCreate.xhtml")).addRule(Join.path("/user").to("/jsf/User.xhtml"))
        .addRule(Join.path("/admin/storageusage").to("/jsf/User.xhtml")).addRule(Join.path("/users").to("/jsf/Users.xhtml"))
        .addRule(Join.path("/usergroup").to("/jsf/UserGroup.xhtml")).addRule(Join.path("/usergroups").to("/jsf/UserGroups.xhtml"))
        .addRule(Join.path("/createusergroup").to("/jsf/UserGroupCreate.xhtml")).addRule(Join.path("/login").to("/jsf/Login.xhtml"))
        .addRule(Join.path("/logout").to("/jsf/Logout.xhtml")).addRule(Join.path("/register").to("/jsf/Register.xhtml"))
        .addRule(Join.path("/pwdreset").to("/jsf/PasswordReset.xhtml")).addRule(Join.path("/subscriptions").to("/jsf/Subscription.xhtml"))

        .addRule(Join.path("/terms_of_use").to("/jsf/TermsOfUse.xhtml"))
        .addRule(Join.path("/privacy_policy").to("/jsf/PrivacyPolicy.xhtml")).addRule(Join.path("/help").to("/jsf/Help.xhtml"))
        .addRule(Join.path("/imprint").to("/jsf/Imprint.xhtml"));
  }



  @Override
  public int priority() {
    return 0;
  }
}
