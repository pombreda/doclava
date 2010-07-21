<table border="1">
<tr>
<th> </th>
<?cs each:site = sites ?>
<th><?cs var:site.name ?></th>
<?cs /each ?>
</tr>
<?cs each:package = packages ?>
  <tr>
  <td><?cs var:package.name ?></td>
  <?cs each:site = package.sites ?>
    <td>
    <?cs if:site.hasPackage ?>
    <a href="<?cs var:site.link ?>"><div style="background:green">&nbsp;</div></a>
    <?cs else ?>
    <div style="background:red">&nbsp;</div>
    <?cs /if ?>
    </td>
  <?cs /each ?>
  </tr>

  <?cs each:class = package.classes ?>
    <tr>
      <td><?cs var:class.qualifiedName ?></td>
      <?cs each:site = class.sites ?>
        <td>
        <?cs if:site.hasClass ?>
        <a href="<?cs var:site.link ?>"><div style="background:green">&nbsp;</div></a>
        <?cs else ?>
        <div style="background:red">&nbsp;</div>
        <?cs /if ?>
        </td>
      <?cs /each ?>
    </tr>

    <?cs each:method = class.methods ?>
    <tr>
      <td><?cs var:method.signature ?></td>
      <?cs each:site = method.sites ?>
        <td>
        <?cs if:site.hasMethod ?>
        <a href="<?cs var:site.link ?>"><div style="background:green">&nbsp;</div></a>
        <?cs else ?>
        <div style="background:red">&nbsp;</div>
        <?cs /if ?>
        </td>
      <?cs /each ?>
    </tr>
    <?cs /each ?><?cs # methods ?>
  <?cs /each ?><?cs # classes ?>
<?cs /each ?><?cs # packages ?>
</table>