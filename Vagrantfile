Vagrant.configure("2") do |config|
  config.vm.box = "bento/ubuntu-24.04"
  config.vm.box_version = "202510.26.0"

  # Karaf
  config.vm.network "forwarded_port", guest: 8181, host: 18181, host_ip: "127.0.0.1"
  # PostgreSQL
  config.vm.network "forwarded_port", guest: 5432, host: 15432, host_ip: "127.0.0.1"
  # Karaf debug, uncomment if needed
  #config.vm.network "forwarded_port", guest: 5005, host:  5005, host_ip: "127.0.0.1"

  config.vm.synced_folder "postgres-scripts", "/home/vagrant/postgres-scripts", create: true

  config.vm.provision "shell", name: "apt", inline: <<-SHELL
    apt update
    apt install -y tree # Utility to print directory structure
  SHELL

  config.vm.provision "shell", name: "java", inline: <<-SHELL
    apt install -y openjdk-21-jdk-headless

    JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
    if [ -z $(grep JAVA_HOME /etc/environment) ]; then
        echo "JAVA_HOME=$JAVA_HOME" >> /etc/environment
    fi
  SHELL

  config.vm.provision "shell", name: "postgres", inline: <<-SHELL
    apt install -y postgresql # Version 16

    cd postgres-scripts

    sudo -u postgres psql -f ./create-user.sql
    sudo -u postgres psql -f ./create-database.sql
    sudo -u postgres psql -f ./grant-database-ownership-to-user.sql

    pg_hba_conf="/etc/postgresql/16/main/pg_hba.conf"
    line="host    collections     flashcards      10.0.2.2/32             scram-sha-256"
    if ! grep --fixed-strings --line-regexp "$line" "$pg_hba_conf"; then
           echo "$line" >> "$pg_hba_conf"
    fi

    sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '*'/" /etc/postgresql/16/main/postgresql.conf
  SHELL

  config.vm.provision "shell", name: "karaf", privileged: false, inline: <<-SHELL
    sudo systemctl stop karaf.service
    sudo systemctl disable karaf.service

    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

    EXPECTED_KARAF_ZIP_SIZE=37108159
    KARAF_ZIP=apache-karaf-4.4.11.tar.gz


    size=$(du -b "$KARAF_ZIP" | cut -f 1)
    size=${size:-0}
    if (( "$size" != "$EXPECTED_KARAF_ZIP_SIZE" )); then
        wget https://archive.apache.org/dist/karaf/4.4.11/apache-karaf-4.4.11.tar.gz
    fi
    rm -r apache-karaf-4.4.11
    tar -xf "$KARAF_ZIP"

    cd apache-karaf-4.4.11/etc

    # Uncomment lines
    sed -i '/#karaf = karaf,_g_:admingroup/s/^#//' users.properties
    sed -i '/admingroup = group,admin,manager,viewer,systembundles,ssh/s/^#//' users.properties

    cd ../bin

    ./start

    until ./client version; do
        ((++counter == 10)) && break
        sleep 1
    done

    ./client -b <<< "feature:repo-add mvn:no.priv.bang.karaf/jersey/LATEST/xml/features"
    ./client -b <<< "feature:install jersey-karaf-feature"
    ./client -b <<< "feature:install war"
    ./client -b <<< "feature:install http"
    ./client -b <<< "feature:install jndi"
    ./client -b <<< "feature:install pax-jdbc-postgresql"
    ./client -b <<< "feature:install pax-jdbc-sqlite"
    ./client -b <<< "bundle:install mvn:org.glassfish.jersey.media/jersey-media-multipart/2.47"
    ./client -b <<< "bundle:install mvn:org.glassfish.jersey.ext/jersey-mvc/2.47"
    ./client -b <<< "bundle:install mvn:org.glassfish.jersey.ext/jersey-mvc-mustache/2.47"
    ./client -b <<< "bundle:install wrap:mvn:com.github.spullara.mustache.java/compiler/0.9.14"
    ./client -b <<< "bundle:install mvn:org.jvnet.mimepull/mimepull/1.9.15"
    ./client -b <<< "bundle:install mvn:com.univocity/univocity-parsers/2.9.1"

    ./client -b <<< "feature:install jdbc"
    ./client -b <<< "jdbc:ds-create -dc org.postgresql.Driver -url jdbc:postgresql://localhost:5432/collections --username flashcards --password flashcards collections"

    ./client -b <<< "feature:install wrapper"


    ./client -b <<< "wrapper:install"

    ./stop

    chmod -x karaf.service
    sed -i '/Alias=karaf/d' karaf.service
    sudo systemctl enable $(pwd)/karaf.service
    sudo systemctl start karaf.service
  SHELL


  config.vm.provision "shell", name: "flashcard-app", privileged: false, inline: <<-SHELL
    if [ ! -d "flashcard-app/.git" ]; then
        git clone --depth 1 https://github.com/copia-fidei/flashcard-app.git
    else
        cd ~/flashcard-app
        git pull
    fi
    cd ~/flashcard-app

    ./gradlew clean
    ./gradlew bundle

    rm ~/apache-karaf-4.4.11/deploy/flashcard-app-*.jar
    cp build/libs/flashcard-app-*.jar ~/apache-karaf-4.4.11/deploy

  SHELL
end
