FILESEXTRAPATHS:prepend := "${FILE_DIRNAME}/files:"

DESCRIPTION = "Setup script for K3s"

DEBIAN_DEPENDS = "iptables"

inherit dpkg-raw

SRC_URI = " \
    file://k3s \
    file://k3s.service \
    file://k3s.service.env \
    file://postinst \
"

do_install() {
    install -v -d ${D}/etc/systemd/system
    install -v -m 755 ${WORKDIR}/k3s.service ${D}/etc/systemd/system/
    install -v -m 755 ${WORKDIR}/k3s.service.env ${D}/etc/systemd/system/
    install -v -d ${D}/root/
    install -v -m 755 ${WORKDIR}/k3s ${D}/root/
}
