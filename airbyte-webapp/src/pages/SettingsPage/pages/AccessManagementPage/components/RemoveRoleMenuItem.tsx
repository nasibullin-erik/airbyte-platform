import { FormattedMessage, useIntl } from "react-intl";

import { Box } from "components/ui/Box";
import { Text } from "components/ui/Text";

import { useCurrentOrganizationInfo, useCurrentWorkspace, useDeletePermissions } from "core/api";
import { useCurrentUser } from "core/services/auth";
import { useConfirmationModalService } from "hooks/services/ConfirmationModal";

import styles from "./RemoveRoleMenuItem.module.scss";
import { ResourceType, UnifiedUserModel } from "./util";

interface RemoveRoleMenuItemProps {
  user: UnifiedUserModel;
  resourceType: ResourceType;
}

export const RemoveRoleMenuItem: React.FC<RemoveRoleMenuItemProps> = ({ user, resourceType }) => {
  const { openConfirmationModal, closeConfirmationModal } = useConfirmationModalService();

  let permissionToRemove = "";

  if (resourceType === "organization") {
    permissionToRemove = user.organizationPermission?.permissionId ?? "";
  } else if (!user.organizationPermission || user.organizationPermission?.permissionType === "organization_member") {
    permissionToRemove = user.workspacePermission?.permissionId ?? "";
  }

  const organizationName = useCurrentOrganizationInfo()?.organizationName;
  const { name: workspaceName } = useCurrentWorkspace();
  const { formatMessage } = useIntl();
  const { userId: currentUserId } = useCurrentUser();
  const resourceName = resourceType === "organization" ? organizationName : workspaceName;
  const nameToDisplay = user.userName || user.userEmail;
  const { mutateAsync: removePermission } = useDeletePermissions();

  const onClick = () =>
    openConfirmationModal({
      text: formatMessage(
        { id: "settings.accessManagement.removePermissions" },
        { user: nameToDisplay, resource: resourceName }
      ),
      title: formatMessage({ id: "settings.accessManagement.removeUser" }),
      submitButtonText: formatMessage({ id: "settings.accessManagement.removeUser" }),
      onSubmit: async () => {
        await removePermission(permissionToRemove);
        closeConfirmationModal();
      },
      submitButtonDataId: "remove",
    });

  return (
    <button
      onClick={onClick}
      disabled={currentUserId === user.id || permissionToRemove.length === 0}
      className={styles.removeRoleMenuItem__button}
    >
      <Box py="lg" px="md">
        <Text color={currentUserId === user.id || permissionToRemove.length === 0 ? "red200" : "red"}>
          <FormattedMessage id="settings.accessManagement.removeUser" />
        </Text>
      </Box>
    </button>
  );
};
