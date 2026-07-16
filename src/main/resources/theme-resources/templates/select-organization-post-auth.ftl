<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true displayInfo=false; section>
    <#if section = "title">
        Select Organization
    <#elseif section = "form">
        <div id="kc-select-org-heading">
            <h1>Select Organization</h1>
            <#if username?? && username != "">
                <p>Signed in as <strong>${username}</strong>. Choose an organization to continue.</p>
            </#if>
        </div>

        <form action="${url.loginAction}" method="post">
            <div id="kc-user-organizations">
                <ul class="org-list" style="list-style:none;padding:0;margin:1rem 0;">
                    <#if organizations??>
                        <#list organizations as org>
                            <li style="margin-bottom:0.5rem;">
                                <a id="organization-${org.alias}"
                                   href="javascript:void(0)"
                                   onclick="document.forms[0]['kc.org'].value='${org.alias}';document.forms[0].requestSubmit()"
                                   style="display:flex;align-items:center;gap:0.75rem;padding:0.75rem 1rem;border:1px solid #ddd;border-radius:8px;text-decoration:none;color:inherit;cursor:pointer;">
                                    <span style="display:inline-flex;align-items:center;justify-content:center;width:36px;height:36px;border-radius:50%;background:#6B9AB8;color:#fff;font-weight:600;font-size:1rem;">
                                        ${(org.name!org.alias)?substring(0,1)?upper_case}
                                    </span>
                                    <span>${org.name!org.alias}</span>
                                </a>
                            </li>
                        </#list>
                    </#if>
                </ul>
            </div>
            <input type="hidden" name="kc.org" />
        </form>
    </#if>
</@layout.registrationLayout>
