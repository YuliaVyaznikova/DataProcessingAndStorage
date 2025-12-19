<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes"/>

  <xsl:key name="byId" match="person" use="@id"/>

  <xsl:template match="/people">
    <html>
      <head>
        <meta charset="UTF-8"/>
        <title>Report</title>
        <style>
          body { font-family: Arial, sans-serif; margin: 24px; }
          h1 { margin-top: 0; }
          h2 { margin-bottom: 4px; }
          .section { margin-bottom: 24px; }
          .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; }
          .card { border: 1px solid #ddd; border-radius: 8px; padding: 12px; }
          .muted { color: #666; font-size: 0.9em; }
          ul { margin: 6px 0 0 18px; }
          li { margin: 2px 0; }
        </style>
      </head>
      <body>
        <h1>Person with parents, grand-parent and siblings</h1>
        <xsl:variable name="candidate" select="person[(parents/father and parents/mother) and (siblings/brother and siblings/sister) and ((parents/father/@ref and key('byId', parents/father/@ref)/parents/*) or (parents/mother/@ref and key('byId', parents/mother/@ref)/parents/*))][1]"/>
        <xsl:choose>
          <xsl:when test="$candidate">
            <div class="section">
              <h2 class="muted">Selected person</h2>
              <div class="card">
                <xsl:call-template name="person-card">
                  <xsl:with-param name="p" select="$candidate"/>
                </xsl:call-template>
              </div>
            </div>
            <div class="section">
              <h2>Father</h2>
              <div class="grid">
                <xsl:for-each select="$candidate/parents/father">
                  <div class="card">
                    <xsl:call-template name="render-ref-or-name">
                      <xsl:with-param name="node" select="."/>
                    </xsl:call-template>
                  </div>
                </xsl:for-each>
              </div>
            </div>
            <div class="section">
              <h2>Mother</h2>
              <div class="grid">
                <xsl:for-each select="$candidate/parents/mother">
                  <div class="card">
                    <xsl:call-template name="render-ref-or-name">
                      <xsl:with-param name="node" select="."/>
                    </xsl:call-template>
                  </div>
                </xsl:for-each>
              </div>
            </div>
            <div class="section">
              <h2>Brothers</h2>
              <p class="muted">Names: 
                <xsl:call-template name="list-names-ref-only">
                  <xsl:with-param name="nodes" select="$candidate/siblings/brother"/>
                </xsl:call-template>
              </p>
              <div class="grid">
                <xsl:for-each select="$candidate/siblings/brother">
                  <div class="card">
                    <xsl:call-template name="render-ref-only">
                      <xsl:with-param name="node" select="."/>
                    </xsl:call-template>
                  </div>
                </xsl:for-each>
              </div>
            </div>
            <div class="section">
              <h2>Sisters</h2>
              <p class="muted">Names: 
                <xsl:call-template name="list-names-ref-only">
                  <xsl:with-param name="nodes" select="$candidate/siblings/sister"/>
                </xsl:call-template>
              </p>
              <div class="grid">
                <xsl:for-each select="$candidate/siblings/sister">
                  <div class="card">
                    <xsl:call-template name="render-ref-only">
                      <xsl:with-param name="node" select="."/>
                    </xsl:call-template>
                  </div>
                </xsl:for-each>
              </div>
            </div>
          </xsl:when>
          <xsl:otherwise>
            <p>No person found.</p>
          </xsl:otherwise>
        </xsl:choose>
      </body>
    </html>
  </xsl:template>

  <xsl:template name="person-card">
    <xsl:param name="p"/>
    <xsl:variable name="nameText" select="normalize-space(concat($p/first-name, ' ', $p/last-name))"/>
    <xsl:variable name="idText" select="$p/@id"/>
    <div>
      <strong>
        <xsl:choose>
          <xsl:when test="$nameText != ''">
            <xsl:value-of select="$nameText"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$idText"/>
          </xsl:otherwise>
        </xsl:choose>
      </strong>
      <span class="muted">
        <xsl:text> (</xsl:text>
        <xsl:choose>
          <xsl:when test="$nameText != ''"><xsl:value-of select="$idText"/></xsl:when>
          <xsl:otherwise><xsl:value-of select="$nameText"/></xsl:otherwise>
        </xsl:choose>
        <xsl:text>)</xsl:text>
      </span>
      <span class="muted">
        <xsl:text> </xsl:text>
        <xsl:value-of select="$p/gender"/>
      </span>
      <div>
        <ul>
          <li>
            <span>Father:</span>
            <xsl:text> </xsl:text>
            <xsl:call-template name="list-names-ref-or-name">
              <xsl:with-param name="nodes" select="$p/parents/father"/>
            </xsl:call-template>
          </li>
          <li>
            <span>Mother:</span>
            <xsl:text> </xsl:text>
            <xsl:call-template name="list-names-ref-or-name">
              <xsl:with-param name="nodes" select="$p/parents/mother"/>
            </xsl:call-template>
          </li>
          <li>
            <span>Brothers:</span>
            <xsl:text> </xsl:text>
            <xsl:call-template name="list-names-ref-only">
              <xsl:with-param name="nodes" select="$p/siblings/brother"/>
            </xsl:call-template>
          </li>
          <li>
            <span>Sisters:</span>
            <xsl:text> </xsl:text>
            <xsl:call-template name="list-names-ref-only">
              <xsl:with-param name="nodes" select="$p/siblings/sister"/>
            </xsl:call-template>
          </li>
          <li>
            <span>Sons:</span>
            <xsl:text> </xsl:text>
            <xsl:call-template name="list-names-ref-only">
              <xsl:with-param name="nodes" select="$p/children/son"/>
            </xsl:call-template>
          </li>
          <li>
            <span>Daughters:</span>
            <xsl:text> </xsl:text>
            <xsl:call-template name="list-names-ref-only">
              <xsl:with-param name="nodes" select="$p/children/daughter"/>
            </xsl:call-template>
          </li>
          <li>
            <span>Grand-mothers:</span>
            <xsl:text> </xsl:text>
            <xsl:call-template name="list-grand-parents">
              <xsl:with-param name="p" select="$p"/>
              <xsl:with-param name="type" select="'mother'"/>
            </xsl:call-template>
          </li>
          <li>
            <span>Grand-fathers:</span>
            <xsl:text> </xsl:text>
            <xsl:call-template name="list-grand-parents">
              <xsl:with-param name="p" select="$p"/>
              <xsl:with-param name="type" select="'father'"/>
            </xsl:call-template>
          </li>
          <li>
            <span>Uncles:</span>
            <xsl:text> </xsl:text>
            <xsl:call-template name="list-uncles-aunts">
              <xsl:with-param name="p" select="$p"/>
              <xsl:with-param name="type" select="'brother'"/>
            </xsl:call-template>
          </li>
          <li>
            <span>Aunts:</span>
            <xsl:text> </xsl:text>
            <xsl:call-template name="list-uncles-aunts">
              <xsl:with-param name="p" select="$p"/>
              <xsl:with-param name="type" select="'sister'"/>
            </xsl:call-template>
          </li>
        </ul>
      </div>
    </div>
  </xsl:template>

  <xsl:template name="full-name">
    <xsl:param name="p"/>
    <xsl:choose>
      <xsl:when test="$p/first-name or $p/last-name">
        <xsl:value-of select="concat(normalize-space($p/first-name), ' ', normalize-space($p/last-name))"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$p/@id"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="render-ref-or-name">
    <xsl:param name="node"/>
    <xsl:choose>
      <xsl:when test="$node/@ref">
        <xsl:variable name="pp" select="key('byId', $node/@ref)"/>
        <xsl:call-template name="person-card">
          <xsl:with-param name="p" select="$pp"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <div>
          <strong><xsl:value-of select="$node/@name"/></strong>
          <span class="muted">
            <xsl:text> </xsl:text>
            <xsl:choose>
              <xsl:when test="name($node)='father'">male</xsl:when>
              <xsl:when test="name($node)='mother'">female</xsl:when>
              <xsl:otherwise/>
            </xsl:choose>
          </span>
        </div>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="render-ref-only">
    <xsl:param name="node"/>
    <xsl:variable name="pp" select="key('byId', $node/@ref)"/>
    <xsl:call-template name="person-card">
      <xsl:with-param name="p" select="$pp"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="list-names-ref-or-name">
    <xsl:param name="nodes"/>
    <xsl:for-each select="$nodes">
      <xsl:if test="position() &gt; 1">, </xsl:if>
      <xsl:choose>
        <xsl:when test="@ref">
          <xsl:variable name="pp" select="key('byId', @ref)"/>
          <xsl:call-template name="full-name">
            <xsl:with-param name="p" select="$pp"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@name"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="list-names-ref-only">
    <xsl:param name="nodes"/>
    <xsl:for-each select="$nodes">
      <xsl:if test="position() &gt; 1">, </xsl:if>
      <xsl:variable name="pp" select="key('byId', @ref)"/>
      <xsl:call-template name="full-name">
        <xsl:with-param name="p" select="$pp"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="list-grand-parents">
    <xsl:param name="p"/>
    <xsl:param name="type"/>
    <xsl:variable name="parents" select="$p/parents/*[@ref]"/>
    <xsl:variable name="gp" select="key('byId', $parents/@ref)/parents/*[name()=$type]"/>
    <xsl:for-each select="$gp">
      <xsl:if test="position() &gt; 1">, </xsl:if>
      <xsl:choose>
        <xsl:when test="@ref">
          <xsl:variable name="pp" select="key('byId', @ref)"/>
          <xsl:call-template name="full-name">
            <xsl:with-param name="p" select="$pp"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@name"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="list-uncles-aunts">
    <xsl:param name="p"/>
    <xsl:param name="type"/>
    <xsl:variable name="parents" select="$p/parents/*[@ref]"/>
    <xsl:variable name="rels" select="key('byId', $parents/@ref)/siblings/*[name()=$type]"/>
    <xsl:for-each select="$rels">
      <xsl:if test="position() &gt; 1">, </xsl:if>
      <xsl:variable name="pp" select="key('byId', @ref)"/>
      <xsl:call-template name="full-name">
        <xsl:with-param name="p" select="$pp"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

</xsl:stylesheet>
