<?cs def:custom_masthead() ?>
  <div id="header">
    <div id="headerLeft">
      <img src="<?cs var:toroot ?>/assets/google-chrome-logo.png" height="80px" />
      <span id="mastlabel">HardCrore.<span>
    </div>
    <div id="headerRight">
        <?cs call:default_search_box() ?>
        <?cs if:reference && reference.apilevels ?>
          <?cs call:default_api_filter() ?>
        <?cs /if ?>
    </div><!-- headerRight -->
  </div><!-- header -->
<?cs /def ?>
