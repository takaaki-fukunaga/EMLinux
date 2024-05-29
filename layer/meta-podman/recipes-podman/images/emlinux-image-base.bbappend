# Add user
USERS += "emlinux"
USER_emlinux[password] = "emlinux"
USER_emlinux[flags] = "create-home clear-text-password"
USER_emlinux[home] = "/home/emlinux"
USER_emlinux[shell] = "/bin/bash"

# Add packages for Podman
IMAGE_PREINSTALL:append = " \
    ca-certificates \
    connman \
    containernetworking-plugins \
    dbus-x11 \
    podman \
    slirp4netns \
    uidmap \
"
