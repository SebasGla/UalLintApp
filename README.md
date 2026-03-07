##System Requirements

This application is bundled with its own Java Runtime (Java 25) using `jpackage`. **You do not need to install Java on your system to run this app!** However, since this is a compiled 64-bit desktop application with a graphical user interface (JavaFX), your system must meet the following requirements:

### Windows
* **OS:** Windows 10 or Windows 11
* **Architecture:** 64-bit (x64) only.
* **Execution:** Simply extract the downloaded `.zip` file and double-click `UalLintApp.exe`.

### Linux
* **OS:** A modern 64-bit Linux distribution (e.g., Ubuntu 20.04+, Linux Mint, Fedora, Debian).
* **Architecture:** 64-bit (`x86_64` / `amd64`). 
  * *Note: 32-bit systems (`i686`) and ARM processors (like Raspberry Pi) are currently not supported by the pre-built binaries.*
* **Desktop Environment:** A graphical desktop environment (GNOME, KDE, XFCE, etc.) is required to render the JavaFX GUI. It will not run on headless servers without an X11/Wayland setup.
* **Execution:** After extracting the downloaded `.zip` file, you must grant execution permissions to the binary before running it:
  ```bash
  cd UalLintApp/bin
  chmod +x UalLintApp
  ./UalLintApp
