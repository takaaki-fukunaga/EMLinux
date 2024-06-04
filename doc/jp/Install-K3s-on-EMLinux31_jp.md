# EMLinux 3.1にK3sをインストールする方法
本記事では、EMLinux 3.1にK3sをインストールするための手順を示します。

## 目次
- [構成図](#構成図)
- [各ソフトウェアのバージョン](#各ソフトウェアのバージョン)
- [前提条件](#前提条件)
- [EMLinux 3.1のビルドに必要なパッケージのインストール](#emlinux-31のビルドに必要なパッケージのインストール)
- [K3sのダウンロード](#k3sのダウンロード)
- [meta-emlinuxリポジトリのクローン](#meta-emlinuxリポジトリのクローン)
- [レシピのカスタマイズ](#レシピのカスタマイズ)
- [EMLinuxのビルド](#emlinuxのビルド)
- [QEMUでEMLinux 3.1を起動する](#qemuでemlinux-31を起動する)
- [コンテナホストからkubectlを実行する](#コンテナホストからkubectlを実行する)
- [NGINXのPodの作成](#nginxのpodの作成)

## 構成図
### EMLinux 3.1のビルド時
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

### EMLinux 3.1の起動時
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

## 各ソフトウェアのバージョン
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

## 前提条件
1. 以下を参照してください。
   - [前提条件](./Build-EMLinux31_jp.md#前提条件)

## EMLinux 3.1のビルドに必要なパッケージのインストール
1. 以下を参考に、必要なパッケージをインストールしてください。
   - [EMLinux 3.1のビルドに必要なパッケージのインストール](../../doc/jp/Build-EMLinux31_jp.md#emlinux-31のビルドに必要なパッケージのインストール)


## K3sのダウンロード
1. K3sをダウンロードしてください。本手順では、ARM64版のバイナリファイルを使用します。
   ```sh
   wget -q "https://github.com/k3s-io/k3s/releases/download/v1.30.0+k3s1/k3s-arm64" -O ./k3s
   ```
   - 最新のバージョンは以下で確認してください。
     - https://github.com/k3s-io/k3s/releases
1. ARM64版のバイナリファイルであることを確認してください。
   ```
   $ file k3s
   k3s: ELF 64-bit LSB executable, ARM aarch64, version 1 (SYSV), statically linked, Go BuildID=sgjlNVCdbgJadT7Nkgnz/1shQ9VtPwZOY17U5jUlP/oi8kFs5uudpYa6XvT6if/if9gZ0XG95E6uODSd-34, stripped
   ```

## meta-emlinuxリポジトリのクローン
1. リポジトリをクローンするためのディレクトリを作成してください。
   ```sh
   mkdir -p /home/emlinux/github/emlinux/bookworm/k3s/repos
   ```
1. 以下のディレクトリに移動してください。   
   ```sh
   cd /home/emlinux/github/emlinux/bookworm/k3s
   ```   
1. git cloneコマンドで、リポジトリをクローンしてください。
   ```sh
   git clone -b bookworm https://github.com/miraclelinux/meta-emlinux.git repos/meta-emlinux
   ```

## レシピのカスタマイズ
1. 本レポジトリをクローンするため、任意のディレクトリに移動してください。
   ```sh
   cd /home/emlinux/github
   ```
1. git cloneコマンドで、本レポジトリをクローンしてください。
   ```sh
   git clone https://github.com/takaaki-fukunaga/EMLinux.git
   ```
1. [meta-k3s](../../layer/meta-k3s/)ディレクトリを、meta-emlinuxをクローンしたディレクトリに移動してください。
   ```sh
   cp -a EMLinux/layer/meta-k3s /home/emlinux/github/emlinux/bookworm/k3s/repos/
   ```
1. [上記手順](#k3sのダウンロード)でダウンロードしたK3sを以下に保存してください。
   ```
   cp k3s /home/emlinux/github/emlinux/bookworm/k3s/repos/meta-k3s/recipes-k3s/configure-k3s/files
   ```
1. vimコマンドで、k3s.service.envファイルを開いてください。
   ```sh
   vim /home/emlinux/github/emlinux/bookworm/k3s/repos/meta-k3s/recipes-k3s/configure-k3s/files/k3s.service.env
   ```
1. k3s.service.envを環境に合わせて編集してください。
   ```
   HTTP_PROXY=http://your-proxy-server.com:80
   HTTPS_PROXY=http://your-proxy-server.com:80
   NO_PROXY=localhost,127.0.0.1
   ```

## EMLinuxのビルド
1. 以下のディレクトリに移動してください。
   ```sh
   cd /home/emlinux/github/emlinux/bookworm/k3s/repos/meta-emlinux/docker
   ```
1. 上記dockerディレクトリ内にあるrun.shを実行してください。初回起動時にはコンテナの作成処理が行われます。コンテナ作成処理後、コンテナにログインした状態になります。
   ```
   $ ./run.sh
   (snip)
   build@82fa043f8378:~/work$
   ```
1. 以下のコマンドを実行し、ビルドのために必要な環境変数を設定してください。
   ```sh
   source repos/meta-emlinux/scripts/setup-emlinux build 
   ```
1. viコマンドで、conf/bblayers.confの末尾に以下を追記してください。
   ```
   BBLAYERS += "${TOPDIR}/../repos/meta-k3s"
   ```   
1. 以下のコマンドを実行し、meta-dockerが追加されていることを確認してください。
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
1. viエディタでlocal.confファイルを開き、ファイルの末尾に以下を追記してください。
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
1. EMLinuxのビルドを行ってください。
   ```sh
   bitbake emlinux-image-base
   ```
1. ビルド完了後、exitコマンドでコンテナからログアウトしてください。
   ```
   build@82fa043f8378:~/work$ exit 
   exit
   ```
## QEMUでEMLinux 3.1を起動する
1. 以下のパッケージをインストールしてください。
   ```sh
   sudo apt install -y qemu-system-arm seabios libvirt-daemon-system
   ```
1. TAPデバイス (以下の実行例ではtap0) を作成してください。
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
1. 以下のディレクトリに移動してください。
   ```sh
   cd /home/emlinux/github/emlinux/bookworm/k3s/build
   ```
1. run.shを作成してください。
   ```sh
   touch run.sh
   ```
   ```sh
   chmod +x run.sh
   ```
1. vimなどでrun.shを以下のように編集してください。
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
1. sudo権限を持ったユーザで、以下を実行してください。
   ```sh
   sudo ./run.sh
   ```
   - パスワードを問われる場合には、sudo権限を持ったユーザのパスワードを入力してください。
1. rootユーザでログインしてください。既定のパスワードはrootです。
   ```
   EMLinux3 login: root
   Password: 
   ```
1. K3sが起動していることを確認してください。
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
1. 以下のコマンドを実行し、コマンド出力結果を、一時的に任意のテキストファイルに保存してください。
   ```sh
   ./k3s kubectl config view --raw
   ```

## コンテナホストからkubectlを実行する
1. コンテナホスト (本記事ではUbuntu Server) にて、kubectlコマンドを実行するためのユーザを作成してください。本記事ではkubeuerという名前のユーザを作成しました。
1. kubeuserでログインし、ホームディレクトリ (e.g., /home/kubeuser) に、.kubeディレクトリを作成してください。
   ```sh
   mkdir .kube
   ```
1. vimなどで.kube/configを作成し、`./k3s kubectl config view --raw`の実行結果を貼り付けてください。また、IPアドレスが、127.0.0.1となっている個所を、EMLinuxのIPアドレス (本記事の環境では192.168.122.77でした) に変更してください。
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
1. kubectlをコンテナホストの任意の場所にダウンロードしてください。
   ```sh
   curl -LO https://dl.k8s.io/release/v1.30.0/bin/linux/amd64/kubectl
   ```
   - 参考: https://kubernetes.io/ja/docs/tasks/tools/install-kubectl-linux/#install-kubectl-binary-with-curl-on-linux
1. kubectlがあるディレクトリにて、以下を実行し、k3sの状態を取得できることを確認してください。
   ```sh
   ./kubectl get node
   ```

## NGINXのPodの作成
1. kubectlがあるディレクトリにて、yamlディレクトリを作成してください。
   ```sh
   mkdir yaml
   ```
1. nginx.yamlファイルを作成し、以下のように編集してください。
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
1. NGINXのPodと、ポートフォワードのためのServiceを作成してください。
   ```sh
   kubectl apply -f nginx.yaml
   ```
1. PodとServiceが動いていることを確認してください。
   ```
   $ ./kubectl get pod,svc
   NAME        READY   STATUS    RESTARTS   AGE
   pod/nginx   1/1     Running   0          84s

   NAME                 TYPE        CLUSTER-IP    EXTERNAL-IP   PORT(S)        AGE
   service/kubernetes   ClusterIP   10.43.0.1     <none>        443/TCP        44m
   service/nginx        NodePort    10.43.22.94   <none>        80:30080/TCP   83s
   ```
1. NGINXにアクセスできることを確認してください。
   ```sh
   curl 192.168.122.77:30080 --noproxy 192.168.122.77
   ```
   - 本記事の環境ではproxyサーバがあるため、`--noproxy`を指定しています。