/*
 * Copyright 2011 Stephen Connolly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.mojo.mrm.api.maven;

import java.io.Serializable;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class Artifact
    implements Serializable, Comparable
{

    private static final long serialVersionUID = 1L;

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final String type;

    private final String classifier;

    private final Long timestamp;

    private final Integer buildNumber;

    private transient String name;

    private transient String timestampVersion;

    private Artifact( String groupId, String artifactId, String version, String classifier, String type, Long timestamp,
                      Integer buildNumber )
    {
        groupId.getClass();
        artifactId.getClass();
        version.getClass();
        type.getClass();
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.classifier = classifier;
        this.timestamp = isSnapshot() ? timestamp : null;
        this.buildNumber = isSnapshot() ? buildNumber : null;
    }

    public Artifact( String groupId, String artifactId, String version, String classifier, String type, long timestamp,
                     int buildNumber )
    {
        this( groupId, artifactId, version, classifier, type, new Long( timestamp ), new Integer( buildNumber ) );
    }

    public Artifact( String groupId, String artifactId, String version, String type, long timestamp, int buildNumber )
    {
        this( groupId, artifactId, version, null, type, new Long( timestamp ), new Integer( buildNumber ) );
    }

    public Artifact( String groupId, String artifactId, String version, String classifier, String type )
    {
        this( groupId, artifactId, version, classifier, type, null, null );
    }

    public Artifact( String groupId, String artifactId, String version, String type )
    {
        this( groupId, artifactId, version, null, type );
    }

    public String getName()
    {
        if ( name == null )
        {
            name = MessageFormat.format( "{0}-{1}{2}.{3}", new Object[]{ artifactId, getTimestampVersion(),
                ( classifier == null ? "" : "-" + classifier ), type } );
        }
        return name;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getType()
    {
        return type;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public Long getTimestamp()
    {
        return timestamp;
    }

    public String getTimestampString()
    {
        if ( timestamp == null )
        {
            return null;
        }
        else
        {
            SimpleDateFormat fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
            fmt.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
            return fmt.format( new Date( timestamp.longValue() ) );
        }
    }

    public Integer getBuildNumber()
    {
        return buildNumber;
    }

    public boolean isSnapshot()
    {
        return version.endsWith( "-SNAPSHOT" );
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        Artifact artifact = (Artifact) o;

        if ( !groupId.equals( artifact.groupId ) )
        {
            return false;
        }
        if ( !artifactId.equals( artifact.artifactId ) )
        {
            return false;
        }
        if ( !version.equals( artifact.version ) )
        {
            return false;
        }
        if ( !type.equals( artifact.type ) )
        {
            return false;
        }
        if ( classifier != null ? !classifier.equals( artifact.classifier ) : artifact.classifier != null )
        {
            return false;
        }
        if ( buildNumber != null ? !buildNumber.equals( artifact.buildNumber ) : artifact.buildNumber != null )
        {
            return false;
        }
        if ( timestamp != null ? !timestamp.equals( artifact.timestamp ) : artifact.timestamp != null )
        {
            return false;
        }

        return true;
    }

    public boolean equalSnapshots( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        Artifact artifact = (Artifact) o;

        if ( !groupId.equals( artifact.groupId ) )
        {
            return false;
        }
        if ( !artifactId.equals( artifact.artifactId ) )
        {
            return false;
        }
        if ( !version.equals( artifact.version ) )
        {
            return false;
        }
        if ( !type.equals( artifact.type ) )
        {
            return false;
        }
        if ( classifier != null ? !classifier.equals( artifact.classifier ) : artifact.classifier != null )
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + ( classifier != null ? classifier.hashCode() : 0 );
        return result;
    }

    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append( "Artifact" );
        sb.append( "{" ).append( groupId );
        sb.append( ":" ).append( artifactId );
        sb.append( ":" ).append( getTimestampVersion() );
        if ( classifier != null )
        {
            sb.append( ":" ).append( classifier );
        }
        sb.append( ":" ).append( type );
        sb.append( '}' );
        return sb.toString();
    }

    public String getTimestampVersion()
    {
        if ( timestampVersion == null )
        {
            if ( timestamp != null )
            {
                assert isSnapshot();
                SimpleDateFormat fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
                fmt.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
                timestampVersion = MessageFormat.format( "{0}-{1}-{2}", new Object[]{
                    this.version.substring( 0, this.version.length() - "-SNAPSHOT".length() ),
                    fmt.format( new Date( timestamp.longValue() ) ), buildNumber } );
            }
            else
            {
                timestampVersion = version;
            }
        }
        return timestampVersion;
    }

    public int compareTo( Object o )
    {
        Artifact that = (Artifact) o;
        int rv = this.getGroupId().compareTo( that.getGroupId() );
        return rv == 0 ? getName().compareTo( that.getName() ) : rv;
    }
}
