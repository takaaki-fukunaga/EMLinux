# How to Build EMLinux 3.1
This article describes how to build [EMLinux 3.1](https://github.com/miraclelinux/meta-emlinux) and run EMLinux 3.1 OS image (ARM64) with QEMU (qemu-system-aarch64).

## Index
- [Configuration](#configuration)
- [Software Version](#software-version)
- [Prerequisites](#prerequisites)
- [Install Packages to Build EMLinux 3.1](#install-packages-to-build-emlinux-31)
- [Clone meta-emlinux Repository](#clone-meta-emlinux-repository)
- [Build EMLinux 3.1](#build-emlinux-31-2)
- [Run EMLinux 3.1 with QEMU](#run-emlinux-31-with-qemu-2)

## Configuration
### Build EMLinux 3.1
```
+--------------------------------+
| +----------------------------+ |
| | +------------------------+ | |
| | | +--------------------+ | | |
| | | | Build EMLinux 3.1  | | | |
| | | +--------------------+ | | |
| | | Debian                 | | |
| | +------------------------+ | |
| | Docker                     | |
| +----------------------------+ |
| Ubuntu Server                  |
+--------------------------------+
```

### Run EMLinux 3.1 with QEMU
```
+--------------------------------+
| +----------------------------+ |
| | +------------------------+ | |
| | |  EMLinux 3.1           | | |
| | +------------------------+ | |
| | QEMU                       | |
| +-+--------------------------+ |
|   |                            |
| +-+----+                       |
| | tap0 |                       |
| +-+----+                       |
|   |                            |
| +-+----------------------+     |
| | virbr0 (192.168.122.1) |     |
| +------------------------+     |
|                                |
| Ubuntu Server                  |
+--------------------------------+
```

## Software Version
### Ubuntu Server
```
$ cat /etc/lsb-release 
DISTRIB_ID=Ubuntu
DISTRIB_RELEASE=20.04
DISTRIB_CODENAME=focal
DISTRIB_DESCRIPTION="Ubuntu 20.04.6 LTS"
```

### Docker
```
$ docker --version 
Docker version 24.0.5, build 24.0.5-0ubuntu1~20.04.1
```

### Docker Compose
```
$ docker compose version 
Docker Compose version 2.20.2+ds1-0ubuntu1~20.04.1
```

### QEMU
```
$ qemu-system-aarch64 --version 
QEMU emulator version 4.2.1 (Debian 1:4.2-3ubuntu6.28)
Copyright (c) 2003-2019 Fabrice Bellard and the QEMU Project developers
```

## Prerequisites
### Hardware
|||
|-|-|
|OS|Any Linux OS that can run Docker (e.g., Debian, Ubuntu)|
|CPU|Intel 64 or AMD64|
|RAM|8 GiB or more|
|Disk|30 GiB or more|

### Proxy Server
- If you have a proxy server to access to the internet, please do the following steps.

#### .bashrc
- Add the following lines in `.bashrc`.
  ```
  PROXY=http://<your proxy server>:<port number>
  export http_proxy=${PROXY}
  export https_proxy=${PROXY}
  export ftp_proxy=${PROXY}
  export no_proxy='127.0.0.1' 
  ```

#### Docker
- Add the following lines in `/etc/systemd/system/docker.service.d/http-proxy.conf`.
  ```
  [Service]
  Environment="HTTP_PROXY=http://<your proxy server>:<port number>"
  Environment="HTTPS_PROXY=http://<your proxy server>:<port number>"
  ```

#### Git
- Run the following commands.
  ```sh
  git config --global http.proxy http://<your proxy server>:<port number>
  ```
  ```sh
  git config --global https.proxy http://<your proxy server>:<port number>
  ```

### Build EMLinux 3.1
- You can use a normal user who does not have `sudo` privilege. In this article, I have used a normal user `emlinux` and its home directory is `/home/emlinux`.

### Run EMLinux 3.1 with QEMU
- In this article, I have used TAP device so that you need a user has `sudo` privilege.

## Install Packages to Build EMLinux 3.1
1. Install the following packages.
   ```sh
   sudo apt install -y docker.io docker-compose-v2 qemu-user-static
   ```
   - docker.io, docker-compose-v2
     - These packages are used to create a Debian container to build EMLinux 3.1.
   - qemu-user-static
     - This is used for cross compile. In this article, it is used to build ARM64 binaries on the Debian container on Ubuntu Server (x86_64).
       - Container host: x86_64
       - Build target architecture on container guest: ARM64
1. Add the user `emlinux` to the group `docker`.
   ```sh
   sudo gpasswd -a emlinux docker
   ```
1. Logout the session and login again. Check if you can run `docker` command without `sudo`.
   ```sh
   $ docker ps 
   CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
   ```

## Clone meta-emlinux Repository
1. Create a directory to save `meta-emlinux` repository.
   ```sh
   mkdir -p /home/emlinux/github/emlinux/bookworm/misc/repos
   ```
1. Move to the following directory. 
   ```sh
   cd /home/emlinux/github/emlinux/bookworm/misc
   ```   
1. Clone `meta-emlinux` repository.
   ```sh
   git clone -b bookworm https://github.com/miraclelinux/meta-emlinux.git repos/meta-emlinux
   ```

## Build EMLinux 3.1
1. Move to the following directory.
   ```sh
   cd /home/emlinux/github/emlinux/bookworm/misc/repos/meta-emlinux/docker
   ```
1. Execute `run.sh`. At first startup, the script creates the Debian container. After container creation, you login the container.
   ```
   $ ./run.sh
   (snip)
   build@82fa043f8378:~/work$
   ```
1. Run the following command to set environment variabes.
   ```sh
   source repos/meta-emlinux/scripts/setup-emlinux build 
   ```
1. Open `conf/local.conf` with `vi` and add the following lines.
   ```
   $ vi conf/local.conf
   (snip)
   MACHINE = "qemu-arm64"
   ```
1. Run `bitbake` command to build EMLinux.
   ```sh
   bitbake emlinux-image-base
   ```
1. Logout the container.
   ```
   build@82fa043f8378:~/work$ exit 
   exit
   ```

## Run EMLinux 3.1 with QEMU
1. Install the following packages.
   ```sh
   sudo apt install -y qemu-system-arm seabios
   ```
1. Create TAP device (e.g., tap0).
   ```sh
   sudo ip tuntap add tap0 mode tap
   ```
   ```sh
   sudo ip link set tap0 promisc on
   ```
   ```sh
   sudo ip link set dev tap0 master virbr0
   ```
   ```sh
   sudo ip link set dev tap0 up
   ```
1. Move to the following directory.
   ```sh
   cd /home/emlinux/github/emlinux/bookworm/misc/build
   ```
1. Create `run.sh`.
   ```sh
   touch run.sh
   ```
   ```sh
   chmod +x run.sh
   ```
1. Edit `run.sh` as below with `vim` or the other text editor.
   ```
   qemu-system-aarch64 \
   -net nic \
   -net tap,ifname=tap0,script=no \
   -drive id=disk0,file=./tmp/deploy/images/qemu-arm64/emlinux-image-base-emlinux-bookworm-qemu-arm64.ext4,if=none,format=raw \
   -device virtio-blk-device,drive=disk0 -show-cursor -device VGA,edid=on \
   -device qemu-xhci \
   -device usb-tablet \
   -device usb-kbd \
   -object rng-random,filename=/dev/urandom,id=rng0 \
   -device virtio-rng-pci,rng=rng0 \
   -nographic \
   -machine virt \
   -cpu cortex-a57 \
   -m 2G \
   -serial mon:stdio \
   -serial null \
   -kernel ./tmp/deploy/images/qemu-arm64/emlinux-image-base-emlinux-bookworm-qemu-arm64-vmlinux \
   -initrd ./tmp/deploy/images/qemu-arm64/emlinux-image-base-emlinux-bookworm-qemu-arm64-initrd.img \
   -append 'root=/dev/vda rw highres=off console=ttyS0 mem=2G ip=dhcp console=ttyAMA0'
   ```
1. Execute `run.sh` with `sudo` privilege.
   ```sh
   sudo ./run.sh
   ```
1. Login with `root` user. The default password is `root`.
   ```
   EMLinux3 login: root
   Password: 
   ```
