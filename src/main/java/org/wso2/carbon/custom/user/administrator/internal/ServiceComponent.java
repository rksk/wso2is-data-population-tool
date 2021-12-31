package org.wso2.carbon.custom.user.administrator.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.custom.user.administrator.BulkUserUploadThread;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import static org.wso2.carbon.custom.user.administrator.Constants.BULK_UPLOAD;
import static org.wso2.carbon.custom.user.administrator.Constants.BULK_UPLOAD_LOG_PREFIX;


@Component(name = "org.wso2.carbon.identity.custom.user.list.component",
           immediate = true)
public class ServiceComponent {

    private static final Log log = LogFactory.getLog(ServiceComponent.class);
    private static RealmService realmService;

    @Activate
    protected void activate(ComponentContext context) {

        try {
            boolean bulkUpload = Boolean.parseBoolean(System.getProperty(BULK_UPLOAD));
            if (bulkUpload) {
                log.info(BULK_UPLOAD_LOG_PREFIX + "Bulk user upload is started.");
                new BulkUserUploadThread().call();
                log.info(BULK_UPLOAD_LOG_PREFIX + "Bulk user upload is finished.");
            }
        } catch (Throwable e) {
            log.error("An error occurred in the bulk import tool.", e);
        }
        log.info("Custom bulk user upload component is activated.");
    }

    @Deactivate
    protected void deactivate(ComponentContext cxt) {

        log.info("Custom component is deactivated.");
    }

    @Reference(name = "realm.service",
               service = org.wso2.carbon.user.core.service.RealmService.class,
               cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.DYNAMIC,
               unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the Realm Service");
        }
        this.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {

        if (log.isDebugEnabled()) {
            log.debug("Unset the Realm Service.");
        }
        this.realmService = null;
    }

    public static RealmService getRealmService() {
        return realmService;
    }
}
