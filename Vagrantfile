Vagrant.configure("2") do |config|
  config.vm.box = "bento/ubuntu-24.04"
  config.vm.box_version = "202510.26.0"

  config.vm.network "forwarded_port", guest: 8181, host: 18181, host_ip: "127.0.0.1"
  config.vm.network "forwarded_port", guest: 5432, host: 15432, host_ip: "127.0.0.1"
  config.vm.synced_folder "postgres-scripts", "/home/vagrant/postgres-scripts", create: true

  config.vm.provision "shell", name: "apt", inline: <<-SHELL
    sudo apt update
    sudo apt install -y postgresql
    sudo apt install -y openjdk-21-jdk-headless
    sudo apt install -y tree

    JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
    if [ -z $(grep JAVA_HOME /etc/environment) ]; then
        echo "JAVA_HOME=$JAVA_HOME" >> /etc/environment
    fi

    cd postgres-scripts

    sudo -u postgres psql -f ./create-user_postgres.sql
    sudo -u postgres psql -f ./create-database_postgres.sql
    sudo -u postgres psql -f ./grant-database-ownership-to-user_postgres.sql -d collections

    line = "host    collections     flashcards      10.0.2.2/32             scram-sha-256"
    if [ -z $(grep "$line" /etc/postgresql/16/main/pg_hba.conf) ]; then
           echo "$line" >> /etc/postgresql/16/main/pg_hba.conf
    fi
  SHELL

  config.vm.provision "shell", name: "karaf", privileged: false, inline: <<-SHELL
    sudo systemctl stop karaf.service
    sudo systemctl disable karaf.service

    source /etc/environment

    EXPECTED_KARAF_ZIP_SIZE=37108159
    KARAF_ZIP=apache-karaf-4.4.11.tar.gz


    size=$(du -b "$KARAF_ZIP" | cut -f 1)
    size=${size:-0}
    if [ "$size" -ne "$EXPECTED_KARAF_ZIP_SIZE" ]; then
        wget https://archive.apache.org/dist/karaf/4.4.11/apache-karaf-4.4.11.tar.gz
    fi
    rm -r apache-karaf-4.4.11
    tar -xvf "$KARAF_ZIP"

    cd apache-karaf-4.4.11/etc

    # Uncomment lines
    sed -i '/#karaf = karaf,_g_:admingroup/s/^#//' users.properties
    sed -i '/admingroup = group,admin,manager,viewer,systembundles,ssh/s/^#//' users.properties

    cd ../bin

    ./start

    until ./client version; do
        ((++counter == 15)) && break
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
