def setup(app):
    app.add_crossref_type('ini-section', 'ini-section', indextemplate='single: %s')
    app.add_crossref_type('ini-key', 'ini-key', indextemplate='single: %s')
    app.add_crossref_type('env', 'env', indextemplate='single: %%%s%%')
    # I know that there's 'envvar' (indextemplate 'environment variable; %s'
    # but it's no good with colons (warning) and isn't this compact
