# Example project: library suite which gets published to Maven Central

Example of a library suite.

It serves as an opinionated best practice for how to structure such a use-case, 
in particular with focus on Maven.


## Features

### Maven CI friendly versioning

Using the [feature](https://maven.apache.org/guides/mini/guide-maven-ci-friendly.html) introduced with Maven 3.5
makes your process soooo much simpler. No more [Release Plugin](https://maven.apache.org/maven-release/maven-release-plugin/index.html), 
no more [Versions Plugin](https://www.mojohaus.org/versions/versions-maven-plugin/index.html). 

Instead, just use your CI platform's _release_ feature. 

In this project, we are executing `mvn deploy` if someone pressed the "Release" button. For anything else, simple
commits, PRs, etc, we execute `mvn verify`.  Figuring out to do `verify` or `deploy` is handled by 
the [maven-execution.sh](.github/scripts/maven-execution.sh) script. The script does a bit more than that which 
is basically just some extra bells and whistles like supporting non-production releases (aka snapshot releases). 
Steal the ideas and customize to your heart's desire.

Note, that for multi-module projects which use the _Maven CI Friendly Versioning_ feature you must use
the [Flatten Maven Plugin](https://www.mojohaus.org/flatten-maven-plugin/) too. Don't worry about this. 
It works well. And it will not be necessary in Maven 4.

### Use Maven Wrapper

There is really no reason _not_ to use the [Maven Wrapper](https://maven.apache.org/wrapper/) these days.
It makes you independent of the Maven version supplied by your build host. 
Always, always use it. 

Yes, in theory it means that a Maven distribution package needs to downloaded and unpacked.
However, on a platform like GitHub, such assets are cached

### BOM

If your library is really a _suite_ of libraries which can be used together
(example of library suites: Hibernate, Jackson, etc) then you should definitely make
sure to publish a BOM (Bill of Material) too. This makes it a lot easier for the consumers
of your library suite.

In this repo you can find an example of how to do this. A BOM project is really 
just of Maven project with `<packaging>pom</packaging>` and with a `<dependencyManagement>` which
lists the individual libraries of your suite.


Note, that specifically for BOM project, the Flatten Plugin need some extra attention:

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

It is a nice gesture to users if a library exposes static information about itself, for example the library's version,
the library's build time, etc. 

In this repo you can find an example of how to do this. 
It uses the [Templating Maven Plugig](https://www.mojohaus.org/templating-maven-plugin/)
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



## Prerequisites


### POM

- Must be using the [Maven CI Friendly feature](https://maven.apache.org/maven-ci-friendly.html) and the version
element must be `<version>${revision}${sha1}${changelist}</version>`. 
(IMO, all your projects should be using the Maven CI Friendly version paradigm)

- Must have a profile named `release-to-central` and one named `ci`. See the example in this repo.


### GitHub Secrets


### GnuPG key and passphrase for signing the artifact

The following GitHub Secrets must exist:

- `MAVEN_CENTRAL_GPG_SECRET_KEY`. Private key used for signing artifacts published to Maven Central. The value
must be in [TSK format](https://www.ietf.org/archive/id/draft-ietf-openpgp-crypto-refresh-12.html#name-transferable-secret-keys)
You can simply take the output from the `gpg --export-secret-key --armor` command and paste it directly into GitHub UI when
you configure the value for this secret (never mind it is a multi-line value).

- `MAVEN_CENTRAL_GPG_PASSPHRASE`. Passphrase to accompany your private key.


### Credentials for publishing to Maven Central

You must have a Sonatype Nexus token that allows to publish to Maven Central. It is a string which looks like this:`kFCdW7Su:XKAO3FIWcY731jt3rRDIexfrGVqQLFbjqwOJtiGgEQtP`
(first part before colon we refer to as 'username' and second part after colon as 'password')

The following GitHub Secrets must exist:

- `MAVEN_CENTRAL_USERNAME`. Username part of the Sonatype Nexus token you use to publish to Maven Central. (*)
 
- `MAVEN_CENTRAL_PASSWORD`. Password part of the Sonatype Nexus token you use to publish to Maven Central. (*)

*) You can also use your old-style username/password for the Sonatype Nexus UI instead of token. However, Sonatype has announced that 
old-style username/password will eventually stop working for publishing. You still need your old-style username/password to log 
into the Sonatype Nexus UI, though.


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

Simple: Just create a snapshot release, meaning create a release from the GitHub UI with a prerelease suffix. 
This will test that the signing works and that your credentials for Maven Central works. Without creating
a true release.
