# Install K3s on EMLinux 3.1
This article describes how to install K3s on EMLinux 3.1.

## Index
- [Configuration](#configuration)
- [Software Version](#software-version)
- [Prerequisites](#prerequisites)
- [Install Packages to Build EMLinux 3.1](#install-packages-to-build-emlinux-31)
- [Download K3s](#download-k3s)
- [Clone meta-emlinux Repository](#clone-meta-emlinux-repository)
- [Customize Recipes](#customize-recipes)
- [Build EMLinux 3.1](#build-emlinux-31-1)
- [Run EMLinux 3.1 with QEMU](#run-emlinux-31-with-qemu-1)
- [Run kubectl command from Container Host](#run-kubectl-command-from-container-host)
- [Create NGINX Pod](#create-nginx-pod)

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
| | | +--------------------+ | | |
| | | | +----------------+ | | | |
| | | | | NGINX          | | | | |
| | | | +----------------+ | | | |
| | | | K3s                | | | |
| | | +--------------------+ | | |
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

### K3s
```
root@EMLinux3:~/k3s# ./k3s --version 
k3s version v1.30.0+k3s1 (14549535)
go version go1.22.2
```

## Prerequisites
1. Refer to the following page.
   - [Prerequisites](./Build-EMLinux31_en.md#prerequisites)

## Install Packages to Build EMLinux 3.1
1. Refer to the following page.
   - [Install Packages to Build EMLinux 3.1](./Build-EMLinux31_en.md#install-packages-to-build-emlinux-31)

## Download K3s
1. Download K3s.
   ```sh
   wget -q "https://github.com/k3s-io/k3s/releases/download/v1.30.0+k3s1/k3s-arm64" -O ./k3s
   ```
   - You can check the latest version on the following site.
     - https://github.com/k3s-io/k3s/releases
1. Check if the binary file is ARM64.
   ```
   $ file k3s
   k3s: ELF 64-bit LSB executable, ARM aarch64, version 1 (SYSV), statically linked, Go BuildID=sgjlNVCdbgJadT7Nkgnz/1shQ9VtPwZOY17U5jUlP/oi8kFs5uudpYa6XvT6if/if9gZ0XG95E6uODSd-34, stripped
   ```

## Clone meta-emlinux Repository
1. Create a directory to save `meta-emlinux` repository.
   ```sh
   mkdir -p /home/emlinux/github/emlinux/bookworm/k3s/repos
   ```
1. Move to the following directory. 
   ```sh
   cd /home/emlinux/github/emlinux/bookworm/k3s
   ```   
1. Clone `meta-emlinux` repository.
   ```sh
   git clone -b bookworm https://github.com/miraclelinux/meta-emlinux.git repos/meta-emlinux
   ```

## Customize Recipes
1. Move to any directory to save this reository.
   ```sh
   cd /home/emlinux/github
   ```
1. Clone the repository.
   ```sh
   git clone https://github.com/takaaki-fukunaga/EMLinux.git
   ```
1. Copy [meta-k3s](../../layer/meta-k3s/) directory to `repos` directory as below.
   ```sh
   cp -a EMLinux/layer/meta-k3s /home/emlinux/github/emlinux/bookworm/k3s/repos/
   ```
1. Copy `k3s` binary that you have got on the step [Download K3s](#download-k3s) to `files` directory.
   ```
   cp k3s /home/emlinux/github/emlinux/bookworm/k3s/repos/meta-k3s/recipes-k3s/configure-k3s/files
   ```
1. Open `k3s.service.env` with `vim` or the other editor.
   ```sh
   vim /home/emlinux/github/emlinux/bookworm/k3s/repos/meta-k3s/recipes-k3s/configure-k3s/files/k3s.service.env
   ```
1. Add your proxy server information as below.
   ```
   HTTP_PROXY=http://your-proxy-server.com:80
   HTTPS_PROXY=http://your-proxy-server.com:80
   NO_PROXY=localhost,127.0.0.1
   ```

## Build EMLinux 3.1
1. Move to the following directory.
   ```sh
   cd /home/emlinux/github/emlinux/bookworm/k3s/repos/meta-emlinux/docker
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
1. Open `conf/bblayers.conf` with `vi` and add the following lines.
   ```
   BBLAYERS += "${TOPDIR}/../repos/meta-k3s"
   ```   
1. Run the following command to check if `meta-k3s` is added.
   ```sh
   $ bitbake-layers show-layers 
   NOTE: Starting bitbake server...
   layer                 path                                      priority
   ==========================================================================
   meta                  /home/build/work/build/../repos/isar/meta  5
   meta-emlinux          /home/build/work/build/../repos/meta-emlinux  12
   isar-cip-core         /home/build/work/build/../repos/isar-cip-core  6
   meta-k3s              /home/build/work/build/../repos/meta-k3s  30
   ```
1. Open `conf/local.conf` with `vi` and add the following lines.
   ```
   $ vi conf/local.conf
   (snip)
   # ARM64
   MACHINE = "qemu-arm64-k3s"
   
   # Setup for K3s
   IMAGE_INSTALL:append = " configure-k3s"
   
   # Extra space for rootfs in MB
   ROOTFS_EXTRA = "10240"
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
   sudo apt install -y qemu-system-arm seabios libvirt-daemon-system
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
   cd /home/emlinux/github/emlinux/bookworm/k3s/build
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
   -drive id=disk0,file=./tmp/deploy/images/qemu-arm64-k3s/emlinux-image-base-emlinux-bookworm-qemu-arm64-k3s.ext4,if=none,format=raw \
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
   -kernel ./tmp/deploy/images/qemu-arm64-k3s/emlinux-image-base-emlinux-bookworm-qemu-arm64-k3s-vmlinux \
   -initrd ./tmp/deploy/images/qemu-arm64-k3s/emlinux-image-base-emlinux-bookworm-qemu-arm64-k3s-initrd.img \
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
1. Check if K3s is running.
   ```
   # ./k3s kubectl get node
   
   NAME       STATUS   ROLES                  AGE   VERSION
   emlinux3   Ready    control-plane,master   23m   v1.30.0+k3s1
   ```
   ```
   # ./k3s kubectl get pod --all-namespaces 
   NAMESPACE     NAME                                      READY   STATUS      RESTARTS   AGE
   kube-system   coredns-576bfc4dc7-zqxkc                  1/1     Running     0          23m
   kube-system   local-path-provisioner-75bb9ff978-dqthp   1/1     Running     0          23m
   kube-system   metrics-server-557ff575fb-4vfrm           1/1     Running     0          23m
   kube-system   helm-install-traefik-crd-hhfc7            0/1     Completed   0          23m
   kube-system   helm-install-traefik-pqs4r                0/1     Completed   2          23m
   kube-system   svclb-traefik-b3187f0c-tmv4t              2/2     Running     0          21m
   kube-system   traefik-5fb479b77-q84zm                   1/1     Running     0          21m
   ```
1. Run the following command and save the command result on any text file.
   ```sh
   ./k3s kubectl config view --raw
   ```

## Run kubectl command from Container Host
1. Create a user (e.g., `kubeuser`) to run kubectl command on the container host (e.g., Ubuntu Server).
1. Login with `kubeuser` account and create `.kube` directory on the home directory (e.g., `/home/kubeuser`).
   ```sh
   mkdir .kube
   ```
1. Create `.kube/config`, paste the result of `./k3s kubectl config view --raw` and change IP address from `127.0.0.1` to IP address of your EMLinux (in this article, IP address is `192.168.122.77`).
   ```sh
   vim .kube/config
   ```
   ```
   apiVersion: v1
   clusters:
   - cluster:
       certificate-authority-data: (snip)
       server: https://192.168.122.77:6443
     name: default
   contexts:
   - context:
       cluster: default
       user: default
     name: default
   current-context: default
   kind: Config
   preferences: {}
   users:
   - name: default
     user:
       client-certificate-data: (snip)
       client-key-data: (snip)   
   ```
1. Download `kubectl`.
   ```sh
   curl -LO https://dl.k8s.io/release/v1.30.0/bin/linux/amd64/kubectl
   ```
   - Reference: https://kubernetes.io/ja/docs/tasks/tools/install-kubectl-linux/#install-kubectl-binary-with-curl-on-linux
1. Run the following command to check if you can get K3s status.
   ```sh
   ./kubectl get node
   ```

## Create NGINX Pod
1. Create `yaml` directory.
   ```sh
   mkdir yaml
   ```
1. Create `nginx.yaml` file and edit as below.
   ```sh
   vim yaml/nginx.yaml
   ```
   ```yaml
   apiVersion: v1
   kind: Pod
   metadata:
     name: nginx
     labels:
       app: nginx
   spec:
     containers:
       - name: nginx
         image: nginx
         ports:
         - containerPort: 80
   ---
   apiVersion: v1
   kind: Service
   metadata:
     name: nginx
   spec:
     type: NodePort
     ports:
       - name: nginx
         protocol: TCP
         port: 80
         targetPort: 80
         nodePort: 30080
     selector:
       app: nginx
   ```
1. Apply the manifest file to create pod and service.
   ```sh
   kubectl apply -f nginx.yaml
   ```
1. Check if the Pod and Service are runnging.
   ```
   $ ./kubectl get pod,svc
   NAME        READY   STATUS    RESTARTS   AGE
   pod/nginx   1/1     Running   0          84s

   NAME                 TYPE        CLUSTER-IP    EXTERNAL-IP   PORT(S)        AGE
   service/kubernetes   ClusterIP   10.43.0.1     <none>        443/TCP        44m
   service/nginx        NodePort    10.43.22.94   <none>        80:30080/TCP   83s
   ```
1. Check if you can access to NGINX.
   ```sh
   curl 192.168.122.77:30080 --noproxy 192.168.122.77
   ```
   - If you don't have a proxy server, you don't need `--noproxy` option.