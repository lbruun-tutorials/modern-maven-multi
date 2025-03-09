# Example project: library suite which gets published to Maven Central

Example of a library suite.

It serves as an opinionated best practice for how to structure such a use-case, 
in particular with focus on Maven.

The example presented here is a pretend library suite named _serendipity_ which
has a number of modules as well as a BOM. You can use it as a template for your
own library suite.


## Features

### Maven CI friendly versioning

Using the [feature](https://maven.apache.org/guides/mini/guide-maven-ci-friendly.html) introduced with Maven 3.5
makes your release process soooo much simpler. No more [Release Plugin](https://maven.apache.org/maven-release/maven-release-plugin/index.html), 
no more [Versions Plugin](https://www.mojohaus.org/versions/versions-maven-plugin/index.html). 

Instead, just use your CI platform's _Release_ feature. 

In this project, we are executing `mvn deploy` if someone pressed the "Release" button. For anything else, simple
commits, PRs, etc, we execute `mvn verify`.  Figuring out to do `verify` or `deploy` is handled by 
the [maven-execution.sh](.github/scripts/maven-execution.sh) script. The script does a bit more than that which 
is basically just some extra bells and whistles like supporting non-production releases (aka snapshot releases). 
Steal the ideas and customize to your heart's desire. It can be made more simple than what is in this
example.

Note, that for multi-module projects which use the _Maven CI Friendly Versioning_ feature you must use
the [Flatten Maven Plugin](https://www.mojohaus.org/flatten-maven-plugin/) too. Don't worry about this. 
It works well. And it will not be necessary in Maven 4.

### Maven Wrapper

There is really no reason _not_ to use the [Maven Wrapper](https://maven.apache.org/wrapper/) these days.. at least
for library projects. It makes you independent of the Maven version supplied by your build host. 
Always, always use it for library projects.

Yes, in theory it means that a Maven distribution package needs to be downloaded and unpacked for every CI job
execution. However, on a platform like GitHub, such assets are cached and not actually fetched from afar.
Using the Linux `time` command I get 80 milliseconds used for the download of Maven and 70 
milliseconds used for unpacking it. In other words: negligible. Don't worry about it.

To be fair, the Wrapper actually has some dependencies of its own: 
- `wget` (alternatively `curl`) must be available in the PATH.
- `unzip` must be available in the PATH.

but these requirements are typically easy to fulfill for library projects. By contrast, for app projects, you might 
be using some buildpack or what not to build a docker image. In such environment the `wget` and
`unzip` may not be available, so for these type of projects use of the Maven Wrapper might be obstructed. 
In such situation you might want to investigate using the wrapper in the mode with a checked
binary JAR as this mode does not require any external tools.



### BOM

If your library is really a _suite_ of libraries which can be used together
(examples of library suites: Hibernate, Jackson, etc) then you should definitely make
sure to publish a BOM (Bill of Material) too. This makes it a lot easier for the consumers
of your library suite.

In this repo you can find an [example](bom/) of how to do this. A BOM project is really 
just of Maven project with `<packaging>pom</packaging>` and with a `<dependencyManagement>` section which
lists the individual libraries of your suite.

Note, that specifically for a BOM project, the Flatten Plugin need some extra attention:

```xml
      <!-- Specifically for BOMs:
           augment Flatten Plugin's config to make sure the 'dependencyManagement' section
           is correctly constructed in the flattened POM                                -->
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>flatten-maven-plugin</artifactId>
        <configuration>
          <updatePomFile>true</updatePomFile>
          <pomElements>
            <dependencyManagement>expand</dependencyManagement>
          </pomElements>
        </configuration>
      </plugin>
```

### Library information

It is a nice gesture to users if a library exposes static information about itself, available at runtime, 
for example the library's version, the library's build time, etc. 

In this repo you can find an example of how to do this. 
It uses the [Templating Maven Plugin](https://www.mojohaus.org/templating-maven-plugin/)
to construct a class named `LibraryInfo` which has certain data properties which are set as constants
by the Maven build. There are possibly a dozen other ways to do this too. However, I find the methodology presented 
here to be the most convenient. Unfortunately, it leads to a bit of duplication, but you may find a way
around that.

Whatever methodology you choose you should of course be _consistent_ across your suite.
Suggest to always put the information class in the root package of the specific library.


### Project location as variable

Project's sometime move. In particular on in-house GitLab, Bitbucket, etc, I've seen this a lot. But 
it happens on public GitHub projects too.
When this happens there are a lot of places where it needs to be changed.
This repository promotes the idea that this information should be supplied by your CI system 
instead of being static text in your POM.

### Signing with Bouncy Castle, not the GnuPG

If you want to publish into Maven Central then the artifacts must be signed.
Back in the days you would need to rely on the build host having the [GnuPG](https://www.gnupg.org/) binary
available. This was a real hassle in more ways than one. For every new version of GnuPG they
would change something that would make it more secure but which quite often meant that unattended usage
(like in a CI pipeline) would break.

Nowadays, the [Maven JarSigner Plugin](https://maven.apache.org/plugins/maven-jarsigner-plugin/) supports
Bouncy Castle as an alternative. This is pure java and is bundled with the plugin. Much more stable
and much more reliable. Use it!


#### Secret key and passphrase for signing the files

This guide assumes that you already have a secret key and passphrase for signing files with.
You can find lots of information on the internet about how to create this. This may indeed involve using
the GnuPG binaries. However, this is only necessary for _generating_ the key and passphrase. 
In other words: Once generated you will in principle no longer need GnuPG.

In this example project the following GitHub Secrets must exist:

- `MAVEN_CENTRAL_GPG_SECRET_KEY`. Private key used for signing artifacts published to Maven Central. The value
  must be in [TSK format](https://www.ietf.org/archive/id/draft-ietf-openpgp-crypto-refresh-12.html#name-transferable-secret-keys). This is a text format which begins with the text `-----BEGIN PGP PRIVATE KEY BLOCK-----`.
  If using GnuPG you can simply take the output from the `gpg --export-secret-key --armor` command and paste it
  directly into GitHub UI when you configure the value for this secret (never mind it is a multi-line value).

- `MAVEN_CENTRAL_GPG_PASSPHRASE`. Passphrase to accompany your private key.



### .gitattributes

Adding a [.gitattributes](.gitattributes) file to your repository with definitions of line endings is a really good idea.
It prevents files with the "wrong" line ending to get into the repo, _regardless_ of the settings of the 
developer's workstation. This is in particular important for projects where many people collaborate.
Nowadays, even for Windows developers, there is no problem in promoting the rule that all text-based files
should be use Unix-style line endings (`LF`). The only exception is scripts meant to be executed on Windows, 
like `.cmd` or `.bat` files. These should use Windows-style line endings (`CRLF`).


### Deploying to Maven Central

Sonatype is the company that created Central. They set the validation rules as well as define the process
for upload-and-release.

Some background first: Deploying to Central is unlike deploying to your in-house Maven repository. Not only will files 
need to be signed but the process itself is also different as it is not synchronous: Once files are uploaded they
are not necessarily released all the way into Central. This is because the files need to go through a validation
(which may take some minutes) and secondly because the default is that you are supposed to manually (after upload) 'release' 
your files for publication into Central using Sonatype's Web UI. The latter can be automated but unlike the traditional
deployment flow - which ends when files are uploaded - there are clearly more steps involved here.

As of March 2025 there are at least 3 ways to publish your artifacts into Maven Central. Which one to use
largely depends on when your namespace was registered with Central:

- Your namespace was registered with Central _before_ March 2024. Sonatype has given you a URL to publish
to which is either `s01.oss.sonatype.org` (projects after Feb 2021) or `oss.sonatype.org` (projects before Feb 2021).
This is known as the "Sonatype OSSRH", Sonatype Open Source Software Repository Hosting. 
It means you are effectively publishing into a Sonatype Nexus2 instance. You have the following two options:
    - You can use the standard [Maven Deploy Plugin](https://maven.apache.org/plugins/maven-deploy-plugin/). 
      This will upload files one-by-one and is therefore not the most effective solution. It will also not allow
      you to fully automate the process of getting your artifact published all the way into Central:
      After the upload succeeds you will have to log into the OSSRH Web Site and "release" your artifact...
      assuming it has passed validation.
      In any case, if you do choose to use this plugin then definitely use the `deployAtEnd` feature
      if in a multi-module project. You can find more information of how to use this plugin for deploying to Central, below.
    - You can use the [Nexus Staging Plugin](https://github.com/sonatype/nexus-maven-plugins/tree/main/staging/maven-plugin).
      This will allow you to fully automate the process if you so wish. I recommend using it over the
      standard Deploy Plugin. You can find more information of how to use this plugin for deploying to Central, below.
- Your namespace was registered with Central _after_ March 2024. It means your project is on the new "Central Portal"
  solution. This solution no longer supports one-by-one file uploads so use of the standard Deploy Plugin is no longer
  an option. Also, the Nexus Staging Plugin is no longer an option. Instead, upload _must_ be done using their REST API 
  which only supports bundle uploads. 
  There is the [Central Publishing Maven Plugin](https://central.sonatype.org/publish/publish-portal-maven/)
  for your Maven workflow so you do not have to deal with the REST API. The plugin allow you to fully automate 
  the release process if you so wish.
  You can find more information of how to use this plugin for deploying to Central, below.
      
#### Supplying credentials

In a CI workflow you should always favor supplying credentials as environment variables instead of writing them 
to disk. Unfortunately, sometimes a `settings.xml` file is still required, but if so, it should look like this:

```xml
<settings>
    <servers>
        <server>
            <id>maven-central</id>
            <username>${env.MAVEN_CENTRAL_USERNAME}</username>
            <password>${env.MAVEN_CENTRAL_PASSWORD}</password>
        </server>
    </servers>
</settings>
```

As you can see the `settings.xml` simply become a vessel for our credentials. 

Luckily, GitHub's own [setup-java action](https://github.com/actions/setup-java) can create such a dummy settings.xml file 
for us. You can see how to do that [here](.github/workflows/ci.yaml). Note, that this action also has features for
dealing with GnuPG secrets but such feature is no longer relevant now that we are using Bouncy Castle instead.





##### Maven Deploy Plugin - setup

There is no need for a `<distributionManagement>` section in the POM. Not having it promotes the idea that
publication of artifacts should never be done by developer's workstation.

Instead, such values can be supplied on the command line in the CI workflow:

```bash
./mvnw \
    -DdeployAtEnd=true \
    -DaltReleaseDeploymentRepository=maven-central::$mvn_central_release_url \
    -DaltSnapshotDeploymentRepository=maven-central::$mvn_central_snapshot_url \
    deploy
```

where environment variables would depend on when your namespace was registered on Central:

| Registration    | `mvn_central_release_url` | `mvn_central_snapshot_url`                                     |
|-----------------| --------- |----------------------------------------------------------------|
| before Feb 2021 | `https://oss.sonatype.org/service/local/staging/deploy/maven2/` | `https://oss.sonatype.org/content/repositories/snapshots/`     |
| after Feb 2021  | `https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/` | `https://s01.oss.sonatype.org/content/repositories/snapshots/` |




The username/password must be your token credentials from OSSRH. This is different from the username/password
used for logging into the UI.

Assuming you have created a `settings.xml` file as explained in the "Supplying credentials" section above then you simply need to make such environment
variables available to the "mvn deploy" execution. Like this: 

```yaml
      - name: Maven execution
        run: mvnw deploy
        env:
          MAVEN_CENTRAL_USERNAME: ${{ secrets.MAVEN_CENTRAL_USERNAME }} # Must be the OSSRH token username, not the username for the UI
          MAVEN_CENTRAL_PASSWORD: ${{ secrets.MAVEN_CENTRAL_PASSWORD }} # Must be the OSSRH token password, not the password for the UI
```


##### Nexus Staging Plugin (by Sonatype) - setup

There is no need for a `<distributionManagement>` section in the POM. 

The plugin configuration should look something like the below:

```xml
          <plugin>
            <groupId>org.sonatype.plugins</groupId>
            <artifactId>nexus-staging-maven-plugin</artifactId>
            <version>1.7.0</version>
            <extensions>true</extensions> 
            <configuration>
              <serverId>maven-central</serverId>

              <!--
                 Projects which has registered with Sonatype/MavenCentral after February 2021 use
                 "s01.oss.sonatype.org" as the hostname in the URLs below. Change accordingly.
              -->
              <nexusUrl>https://oss.sonatype.org/</nexusUrl>

              <!-- Flip this if you feel confident about your build, and it passes
                   the rules of Maven Central                                       -->
              <autoReleaseAfterClose>false</autoReleaseAfterClose>
            </configuration>
          </plugin>
```

The username/password must be your token credentials from OSSRH. This is different from the username/password
used for logging into the UI.

Assuming you have created a `settings.xml` file as explained in the "Supplying credentials" section above then you 
simply need to make such environment variables available to the "mvn deploy" execution. Like this:

```yaml
      - name: Maven execution
        run: mvnw deploy
        env:
          MAVEN_CENTRAL_USERNAME: ${{ secrets.MAVEN_CENTRAL_USERNAME }} # Must be the OSSRH token username, not the username for the UI
          MAVEN_CENTRAL_PASSWORD: ${{ secrets.MAVEN_CENTRAL_PASSWORD }} # Must be the OSSRH token password, not the password for the UI
```




##### Central Publishing Plugin (by Sonatype) - setup

There is no need for a `<distributionManagement>` section in the POM.

The plugin configuration should look something like the below:

```xml
          <plugin>
            <groupId>org.sonatype.central</groupId>
            <artifactId>central-publishing-maven-plugin</artifactId>
            <version>0.7.0</version>
            <extensions>true</extensions>
            <configuration>
              <publishingServerId>maven-central</publishingServerId>

              <!-- Flip this if you feel confident about your build, and it passes
                   the rules of Maven Central                                       -->
              <autoPublish>false</autoPublish>
            </configuration>
          </plugin>
```

The username/password must be your token credentials from Central Portal. This is different from the username/password
used for logging into the UI.

Assuming you have created a `settings.xml` file as explained in the "Supplying credentials" section above then you 
simply need to make such environment variables available to the "mvn deploy" execution. Like this:

```yaml
      - name: Maven execution
        run: mvnw deploy
        env:
          MAVEN_CENTRAL_USERNAME: ${{ secrets.MAVEN_CENTRAL_USERNAME }} # Must be the Central Portal token username, not the username for the UI
          MAVEN_CENTRAL_PASSWORD: ${{ secrets.MAVEN_CENTRAL_PASSWORD }} # Must be the Central Portal token password, not the password for the UI
```







## Releasing

When the project is ready to have a new release, simply go to the GitHub UI and press "Releases". Choose a tag which
complies with [SemVer](https://semver.org/) and press "Publish release". That is all!

If you add a so-called prerelease suffix (in SemVer terminology this means adding a dash+something after the `X.Y.Z`,
for example `3.9.0-RC1`) then the pipeline will assume you mean a Maven snapshot release and publish to snapshot repository.

You can optionally put a `v` in front of your tags as in `v1.4.8`. It won't become part of the Maven version string. 
Whether you use the `v` or not is up to you. Just be consistent.


Note: Be careful with choosing a tag. Once something is published to Maven Central is can never be retracted. Also, while Git allows
you to delete a tag, the GitHub UI deliberately has no such feature. They've made it hard for you to do such operation .. and
indeed it should be hard!


## How do I know if my flow works? (without publishing)

Simple: Just create a snapshot release, meaning create a release from the GitHub UI with a prerelease suffix,
for example `3.9.0-RC1`. This will test that the signing works and that your credentials for Maven Central works. 
Without creating a true release.
